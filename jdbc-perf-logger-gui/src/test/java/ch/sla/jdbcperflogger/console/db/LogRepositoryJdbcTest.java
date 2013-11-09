/* 
 *  Copyright 2013 Sylvain LAURENT
 *     
 *  Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ch.sla.jdbcperflogger.console.db;

import java.util.UUID;

import javax.annotation.Nullable;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ch.sla.jdbcperflogger.StatementType;
import ch.sla.jdbcperflogger.model.StatementLog;

public class LogRepositoryJdbcTest {
    @Nullable
    private LogRepositoryJdbc repository;

    @Before
    public void setup() {
        repository = new LogRepositoryJdbc("test");
    }

    @After
    public void tearDown() {
        final LogRepositoryJdbc repository2 = repository;
        if (repository2 != null) {
            repository2.dispose();
        }
    }

    @Test
    public void testSetup() {
        // just test setup and teardown
    }

    @Test
    public void testInsertAndRead() {
        final StatementLog log = new StatementLog(UUID.randomUUID(), UUID.randomUUID(), System.currentTimeMillis(),
                StatementType.BASE_NON_PREPARED_STMT, "myrawsql", Thread.currentThread().getName());
        final LogRepositoryJdbc repository2 = repository;
        if (repository2 != null) {
            repository2.addStatementLog(log);
            repository2.addStatementLog(log);
            repository2.getStatementLog(1);
        }
    }
}
