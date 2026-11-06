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

import java.math.BigDecimal;
import java.sql.Blob;
import java.sql.Clob;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ConsoleValueFormatTest {

    @Test
    void csvCellQuotesSeparatorsAndDoublesQuotes() {
        Assertions.assertEquals("", ConsoleService.csvCell(null));
        Assertions.assertEquals("plain", ConsoleService.csvCell("plain"));
        Assertions.assertEquals("测中文", ConsoleService.csvCell("测中文"));
        Assertions.assertEquals("\"a,b\"", ConsoleService.csvCell("a,b"));
        Assertions.assertEquals("\"say \"\"hi\"\"\"", ConsoleService.csvCell("say \"hi\""));
        Assertions.assertEquals("\"line1\nline2\"", ConsoleService.csvCell("line1\nline2"));
        Assertions.assertEquals("\"carriage\rreturn\"", ConsoleService.csvCell("carriage\rreturn"));
    }

    @Test
    void csvCellConvertsNonStrings() {
        Assertions.assertEquals("42", ConsoleService.csvCell(42));
        Assertions.assertEquals("1.50", ConsoleService.csvCell(new BigDecimal("1.50")));
        Assertions.assertEquals("true", ConsoleService.csvCell(true));
    }

    @Test
    void stringifyPassesThroughSimpleTypesAndSummarizesBinary() {
        Assertions.assertEquals("text", ConsoleService.stringify("text"));
        Assertions.assertEquals(7, ConsoleService.stringify(7));
        Assertions.assertEquals(Boolean.FALSE, ConsoleService.stringify(Boolean.FALSE));
        Assertions.assertNull(ConsoleService.stringify(null));
        Assertions.assertEquals("<binary 3 bytes>", ConsoleService.stringify(new byte[] {1, 2, 3}));
        Assertions.assertEquals("2024-01-02", ConsoleService.stringify(java.sql.Date.valueOf("2024-01-02")));
    }

    @Test
    void stringifyTruncatesClobAndSummarizesBlob() {
        StringBuilder big = new StringBuilder();
        for (int i = 0; i < 10_000; i++) {
            big.append('x');
        }
        String data = big.toString();
        Clob clob = (Clob) java.lang.reflect.Proxy.newProxyInstance(
                getClass().getClassLoader(), new Class<?>[] {Clob.class}, (p, m, a) -> {
                    switch (m.getName()) {
                        case "length":
                            return (long) data.length();
                        case "getSubString":
                            return data.substring(((Long) a[0]).intValue() - 1,
                                    ((Long) a[0]).intValue() - 1 + (Integer) a[1]);
                        default:
                            throw new UnsupportedOperationException(m.getName());
                    }
                });
        Object out = ConsoleService.stringify(clob);
        Assertions.assertEquals(8192, ((String) out).length());

        Blob blob = (Blob) java.lang.reflect.Proxy.newProxyInstance(
                getClass().getClassLoader(), new Class<?>[] {Blob.class}, (p, m, a) -> {
                    if ("length".equals(m.getName())) {
                        return 1234L;
                    }
                    throw new UnsupportedOperationException(m.getName());
                });
        Assertions.assertEquals("<binary 1234 bytes>", ConsoleService.stringify(blob));
    }
}
