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

package ch.sla.jdbcperflogger;

import java.util.EnumSet;

public enum StatementType {
    BASE_NON_PREPARED_STMT(1), //
    BASE_PREPARED_STMT(2), //
    NON_PREPARED_QUERY_STMT(3), //
    PREPARED_QUERY_STMT(4), //
    PREPARED_BATCH_EXECUTION(5), //
    NON_PREPARED_BATCH_EXECUTION(6), //
    TRANSACTION(7);

    private static StatementType[] vals;

    private final int id;

    StatementType(final int id) {
        this.id = id;
    }

    static {
        vals = new StatementType[8];
        for (final StatementType type : EnumSet.allOf(StatementType.class)) {
            vals[type.id] = type;
        }
    }

    public int getId() {
        return id;
    }

    public static StatementType fromId(final int id) {
        final StatementType statementType = vals[id];
        if (statementType == null) {
            throw new IllegalArgumentException("unhandled ID for StatementType: " + id);
        }
        return statementType;
    }
}
