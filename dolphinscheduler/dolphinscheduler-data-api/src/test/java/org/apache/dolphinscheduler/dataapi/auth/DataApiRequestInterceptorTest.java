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

package org.apache.dolphinscheduler.dataapi.auth;

import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class DataApiRequestInterceptorTest {

    @Test
    void rejectDmlKeywords() {
        Assertions.assertTrue(DataApiRequestInterceptor.containsDml("delete from t"));
        Assertions.assertTrue(DataApiRequestInterceptor.containsDml("INSERT INTO t VALUES (1)"));
        Assertions.assertTrue(DataApiRequestInterceptor.containsDml("/* c */ drop table t"));
        Assertions.assertTrue(DataApiRequestInterceptor.containsDml("select 1; delete from t"));
        Assertions.assertTrue(DataApiRequestInterceptor.containsDml("select * from t into outfile '/tmp/x'"));
        Assertions.assertFalse(DataApiRequestInterceptor.containsDml("select * from t where name='delete'"));
        Assertions.assertFalse(DataApiRequestInterceptor.containsDml("SELECT id FROM orders LIMIT 10"));
    }

    @Test
    void allowsReadOnlySqlThatMentionsWriteWords() {
        Assertions.assertFalse(DataApiRequestInterceptor.containsDml("select * from t for update"));
        Assertions.assertFalse(DataApiRequestInterceptor.containsDml("select * from t where note = \"insert into x\""));
        Assertions.assertFalse(DataApiRequestInterceptor.containsDml("select count(*) from t -- delete later"));
        Assertions.assertFalse(DataApiRequestInterceptor.containsDml(
                "return db.select(\"\"\"\nselect * from t where a='update'\n\"\"\");"));
    }

    @Test
    void rejectMagicWriteMethods() {
        Assertions.assertTrue(DataApiRequestInterceptor.containsDml("return db.update('update t set a=1')"));
        Assertions.assertTrue(DataApiRequestInterceptor.containsDml("db.osint_mysql.insert('insert into t values(1)')"));
        Assertions.assertTrue(DataApiRequestInterceptor.containsDml("db.delete('delete from t where id=1')"));
        Assertions.assertTrue(DataApiRequestInterceptor.containsDml("db.batchUpdate(['delete from t'])"));
        Assertions.assertTrue(DataApiRequestInterceptor.containsDml("return db.table('t').insert([{a:1}])"));
        Assertions.assertFalse(DataApiRequestInterceptor.containsDml("return db.osint_mysql.select('select 1')"));
        Assertions.assertFalse(DataApiRequestInterceptor.containsDml("return db.select('select 1')"));
    }

    @Test
    void extractsAllReferencedDatasourceKeys() {
        Set<String> keys = DataApiRequestInterceptor.extractDatasourceKeys(
                "var a = db.first_db.select('select 1');\nvar b = db.second_db.select('select 2');");
        Assertions.assertEquals(2, keys.size());
        Assertions.assertTrue(keys.contains("first_db"));
        Assertions.assertTrue(keys.contains("second_db"));

        Set<String> used = DataApiRequestInterceptor.extractDatasourceKeys("db.use('third_db'); return db.select('select 1');");
        Assertions.assertEquals(1, used.size());
        Assertions.assertTrue(used.contains("third_db"));

        Assertions.assertTrue(DataApiRequestInterceptor.extractDatasourceKeys("return db.select('select 1')").isEmpty());
    }

    @Test
    void parsesBearerAuthorization() {
        Assertions.assertEquals("abc", DataApiRequestInterceptor.bearerToken("Bearer abc"));
        Assertions.assertEquals("abc", DataApiRequestInterceptor.bearerToken("bearer   abc  "));
        Assertions.assertEquals("abc", DataApiRequestInterceptor.bearerToken("abc"));
        Assertions.assertNull(DataApiRequestInterceptor.bearerToken("  "));
    }
}
