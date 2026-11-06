package com.lhyf.dataasset;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lhyf.dataasset.om.OmClient;

/**
 * AC-3: every REST surface keeps its original path and response shape after the Boot 3 / layering
 * refactor. OpenMetadata is mocked at the client boundary; all routing, shaping and H2 access is real.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:asset-contract;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1"
})
class ApiContractTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Autowired
    private MockMvc mvc;

    @MockBean
    private OmClient om;

    @Test
    void searchKeepsTotalAndItemsShape() throws Exception {
        when(om.searchTables(anyString(), anyInt(), anyInt())).thenReturn(MAPPER.readTree("""
                {"hits":{"total":{"value":1},"hits":[{"_source":{
                  "fullyQualifiedName":"agri.agri_sat.public.dwd_ndvi_tile","name":"dwd_ndvi_tile",
                  "displayName":"NDVI","description":"tiles",
                  "service":{"name":"agri"},"database":{"name":"agri_sat"},
                  "databaseSchema":{"name":"public"},"entityType":"table"}}]}}"""));
        mvc.perform(get("/data-asset/search").param("q", "ndvi"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].fqn").value("agri.agri_sat.public.dwd_ndvi_tile"))
                .andExpect(jsonPath("$.items[0].service").value("agri"))
                .andExpect(jsonPath("$.items[0].schema").value("public"));
    }

    @Test
    void treeGroupsServiceDatabaseSchemaTables() throws Exception {
        when(om.searchTables(anyString(), anyInt(), anyInt())).thenReturn(MAPPER.readTree("""
                {"hits":{"total":{"value":1},"hits":[{"_source":{
                  "fullyQualifiedName":"agri.agri_sat.public.t","name":"t",
                  "service":{"name":"agri"},"database":{"name":"agri_sat"},
                  "databaseSchema":{"name":"public"}}}]}}"""));
        mvc.perform(get("/data-asset/tree"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].service").value("agri"))
                .andExpect(jsonPath("$[0].databases[0].database").value("agri_sat"))
                .andExpect(jsonPath("$[0].databases[0].schemas[0].schema").value("public"))
                .andExpect(jsonPath("$[0].databases[0].schemas[0].tables", hasSize(1)));
    }

    @Test
    void detailReturnsEntityPlusBrowseFlags() throws Exception {
        when(om.tableByFqn(anyString())).thenReturn(MAPPER.readTree("""
                {"id":"u1","name":"t","fullyQualifiedName":"agri.agri_sat.public.t",
                 "columns":[{"name":"id","dataType":"integer"},{"name":"secret","dataType":"varchar"}]}"""));
        mvc.perform(get("/data-asset/detail").param("fqn", "agri.agri_sat.public.t"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entity.name").value("t"))
                .andExpect(jsonPath("$.browseAllowed").value(false))
                // jackson default-property-inclusion=non_null omits a null entryId (original contract)
                .andExpect(jsonPath("$.entryId").doesNotExist());
    }

    @Test
    void columnsCarryVisibilityAndMaskFlags() throws Exception {
        when(om.tableByFqn(anyString())).thenReturn(MAPPER.readTree("""
                {"id":"u1","fullyQualifiedName":"agri.agri_sat.public.t",
                 "columns":[{"name":"id","dataType":"integer","constraint":"","description":""}]}"""));
        mvc.perform(get("/data-asset/columns").param("fqn", "agri.agri_sat.public.t"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("id"))
                .andExpect(jsonPath("$[0].visible").value(true))
                .andExpect(jsonPath("$[0].masked").value(false));
    }

    @Test
    void lineageTableKeepsRootNodesEdgesEnvelope() throws Exception {
        when(om.lineage(anyString(), anyInt(), anyInt())).thenReturn(MAPPER.readTree("""
                {"nodes":{"n1":{"entity":{"fullyQualifiedName":"a.b.c.t","name":"t","type":"table"}}},
                 "upstreamEdges":{},"downstreamEdges":{}}"""));
        mvc.perform(get("/data-asset/lineage/table").param("fqn", "a.b.c.t"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.root").value("a.b.c.t"))
                .andExpect(jsonPath("$.nodes[0].fqn").value("a.b.c.t"))
                .andExpect(jsonPath("$.edges", hasSize(0)));
    }

    @Test
    void lineageKeysAndRowSwitchRemainReadOnlyLists() throws Exception {
        mvc.perform(get("/data-asset/lineage/keys"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
        mvc.perform(get("/data-asset/lineage/row-switch"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void governanceOverviewKeepsFiveSections() throws Exception {
        when(om.get(anyString())).thenReturn(MAPPER.readTree("{\"data\":[]}"));
        mvc.perform(get("/data-asset/governance/overview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.domains").isArray())
                .andExpect(jsonPath("$.dataProducts").isArray())
                .andExpect(jsonPath("$.glossaryTerms").isArray())
                .andExpect(jsonPath("$.classifications").isArray())
                .andExpect(jsonPath("$.tagsByClassification").exists());
    }

    @Test
    void whitelistEntriesListFromConfigDb() throws Exception {
        mvc.perform(get("/data-asset/browse/entries"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void manualLineageEdgeEchoesCounts() throws Exception {
        when(om.baseUrl()).thenReturn("http://om.test");
        when(om.getOrNull(anyString())).thenReturn(MAPPER.readTree("""
                {"id":"u1","name":"t","fullyQualifiedName":"a.b.c.t","columns":[{"name":"c1"}]}"""));
        when(om.tableRef(anyString())).thenReturn(MAPPER.readTree("""
                {"id":"u1","fullyQualifiedName":"a.b.c.t"}"""));
        mvc.perform(post("/data-asset/lineage/manual")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fromFqn":"a.b.c.t","toFqn":"a.b.c.u",
                                 "columns":[{"from":"c1","to":"c1"}]}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"))
                .andExpect(jsonPath("$.columns").value(1))
                .andExpect(jsonPath("$.registered", hasSize(0)));
    }

    @Test
    void badRequestKeepsCodeMessageShape() throws Exception {
        mvc.perform(post("/data-asset/lineage/manual")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("fromFqn")));
    }

    @Test
    void adminHealthStaysAnonymousAndReportsMode() throws Exception {
        when(om.version()).thenReturn("1.7.0");
        mvc.perform(get("/data-asset/admin/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.configDb.status").value("ok"))
                .andExpect(jsonPath("$.openMetadata.version").value("1.7.0"))
                .andExpect(jsonPath("$.authMode").value("local"));
    }
}
