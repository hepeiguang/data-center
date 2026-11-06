/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.dolphinscheduler.dataapi.console;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Plan §9.2 console guard cases: reads accepted, writes / locking / side-effect fragments denied.
 */
class SqlGuardTest {

    private static final int MAX = 20 * 1024;

    private static void assertAllowed(String sql) {
        SqlGuard.Verdict verdict = SqlGuard.check(sql, MAX);
        Assertions.assertTrue(verdict.isAllowed(), () -> sql + " -> " + verdict.getReason());
    }

    private static void assertDenied(String sql) {
        SqlGuard.Verdict verdict = SqlGuard.check(sql, MAX);
        Assertions.assertFalse(verdict.isAllowed(), () -> sql + " should be denied");
        Assertions.assertNotNull(verdict.getReason());
    }

    @Test
    void acceptsPlainReads() {
        assertAllowed("select 1");
        assertAllowed("select id, name from t_ds_user where state = 1");
        assertAllowed("select * from orders o join users u on o.uid = u.id limit 10 offset 20");
        assertAllowed("select a from (select a from t) x");
    }

    @Test
    void acceptsOptionalTrailingSemicolon() {
        assertAllowed("select 1;");
        assertAllowed("select 1 ;  \n");
    }

    @Test
    void acceptsCteAndSubquery() {
        assertAllowed("with c as (select 1 as a) select a from c");
    }

    @Test
    void acceptsExplainSelect() {
        assertAllowed("explain select 1");
        assertDenied("explain update t set a = 1");
    }

    @Test
    void ignoresLiteralsAndCommentsContainingKeywords() {
        assertAllowed("select 'update t set x=1' as demo_col from t");
        assertAllowed("select id from t where name = 'delete; drop'");
        assertAllowed("select ';'");
        assertAllowed("-- delete from t\nselect 1");
        assertAllowed("/* drop table t */ select 1");
        assertAllowed("select \"for update\" from t");
    }

    @Test
    void deniesWritesAndDdl() {
        assertDenied("update t set a = 1");
        assertDenied("delete from t");
        assertDenied("insert into t values (1)");
        assertDenied("drop table t");
        assertDenied("truncate t");
        assertDenied("create table t (a int)");
    }

    @Test
    void deniesMultiStatement() {
        assertDenied("select 1; select 2");
        assertDenied("select 1; select 2;");
    }

    @Test
    void deniesEmptyAndUnparseable() {
        assertDenied("");
        assertDenied("   ");
        assertDenied(";");
        assertDenied("this is definitely not sql");
    }

    @Test
    void deniesOversizedStatement() {
        StringBuilder sb = new StringBuilder("select 1 from dual where x in (");
        for (int i = 0; i < 4000; i++) {
            sb.append(i).append(",");
        }
        SqlGuard.Verdict verdict = SqlGuard.check(sb.toString(), 64);
        Assertions.assertFalse(verdict.isAllowed());
        Assertions.assertTrue(verdict.getReason().contains("bytes"));
    }

    @Test
    void deniesLockingReadsAndSideEffects() {
        assertDenied("select * from t for update");
        assertDenied("SELECT * FROM T FOR UPDATE");
        assertDenied("select pg_sleep(10)");
        assertDenied("select sleep(1)");
        assertDenied("select benchmark(1000000, md5('a'))");
        assertDenied("select * from t into outfile '/tmp/x'");
        assertDenied("select a into backup from t");
        assertDenied("select dblink_connect('x')");
        assertDenied("set role postgres");
        assertDenied("copy t to '/tmp/x'");
    }

    @Test
    void stripRemovesLiteralsAndComments() {
        Assertions.assertFalse(SqlGuard.stripLiteralsAndComments("select ';'").contains(";"));
        Assertions.assertFalse(SqlGuard.stripLiteralsAndComments("select 1 -- drop t\n").contains("drop"));
        Assertions.assertFalse(SqlGuard.stripLiteralsAndComments("select 1 /* drop t */").contains("drop"));
        Assertions.assertTrue(SqlGuard.stripLiteralsAndComments("select a from t").contains("from"));
    }
}
