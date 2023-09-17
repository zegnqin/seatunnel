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

package org.apache.seatunnel.connectors.seatunnel.iceberg.catalog;

import org.apache.seatunnel.api.table.catalog.DataTypeConvertException;
import org.apache.seatunnel.api.table.catalog.DataTypeConvertor;
import org.apache.seatunnel.api.table.type.DecimalType;
import org.apache.seatunnel.api.table.type.PrimitiveByteArrayType;
import org.apache.seatunnel.api.table.type.SeaTunnelDataType;
import org.apache.seatunnel.api.table.type.SqlType;
import org.apache.seatunnel.common.exception.CommonErrorCode;
import org.apache.seatunnel.connectors.seatunnel.iceberg.exception.IcebergConnectorException;

import org.apache.iceberg.types.Type;
import org.apache.iceberg.types.Types;

import com.google.auto.service.AutoService;

import java.util.Locale;
import java.util.Map;

import static org.apache.seatunnel.api.table.type.BasicType.BOOLEAN_TYPE;
import static org.apache.seatunnel.api.table.type.BasicType.DOUBLE_TYPE;
import static org.apache.seatunnel.api.table.type.BasicType.FLOAT_TYPE;
import static org.apache.seatunnel.api.table.type.BasicType.INT_TYPE;
import static org.apache.seatunnel.api.table.type.BasicType.LONG_TYPE;
import static org.apache.seatunnel.api.table.type.BasicType.STRING_TYPE;
import static org.apache.seatunnel.api.table.type.LocalTimeType.LOCAL_DATE_TIME_TYPE;
import static org.apache.seatunnel.api.table.type.LocalTimeType.LOCAL_DATE_TYPE;
import static org.apache.seatunnel.api.table.type.LocalTimeType.LOCAL_TIME_TYPE;

@AutoService(DataTypeConvertor.class)
public class IcebergDataTypeConvertor implements DataTypeConvertor<String> {

    public static final String PRECISION = "precision";
    public static final String SCALE = "scale";

    @Override
    public SeaTunnelDataType<?> toSeaTunnelType(String connectorDataType) {
        if (connectorDataType == null) {
            return null;
        }
        Type.TypeID typeID = Type.TypeID.valueOf(connectorDataType.toUpperCase(Locale.ROOT));
        return toSeaTunnelType(typeID);
    }

    @Override
    public SeaTunnelDataType<?> toSeaTunnelType(
            String connectorDataType, Map<String, Object> dataTypeProperties)
            throws DataTypeConvertException {
        return toSeaTunnelType(connectorDataType);
    }

    @Override
    public String toConnectorType(
            SeaTunnelDataType<?> seaTunnelDataType, Map<String, Object> dataTypeProperties)
            throws DataTypeConvertException {
        return toConnectorTypeType(seaTunnelDataType, dataTypeProperties).typeId().toString();
    }

    public Type toConnectorTypeType(
            SeaTunnelDataType<?> seaTunnelDataType, Map<String, Object> dataTypeProperties)
            throws DataTypeConvertException {
        SqlType sqlType = seaTunnelDataType.getSqlType();
        switch (sqlType) {
            case STRING:
                return Types.StringType.get();
            case BOOLEAN:
                return Types.BooleanType.get();
            case TINYINT:
            case SMALLINT:
            case INT:
                return Types.IntegerType.get();
            case BIGINT:
                return Types.LongType.get();
            case FLOAT:
                return Types.FloatType.get();
            case DOUBLE:
                return Types.DoubleType.get();
            case DECIMAL:
                return Types.DecimalType.of(
                        (int) dataTypeProperties.get(PRECISION),
                        (int) dataTypeProperties.get(SCALE));
            case BYTES:
                return Types.BinaryType.get();
            case DATE:
                return Types.DateType.get();
            case TIME:
                return Types.TimeType.get();
            case TIMESTAMP:
                return Types.TimestampType.withoutZone();
            default:
                throw new UnsupportedOperationException(
                        String.format("Doesn't support Iceberg type '%s''  yet.", sqlType));
        }
    }

    public SeaTunnelDataType<?> toSeaTunnelType(Type.TypeID typeId) {
        if (typeId == null) {
            return null;
        }
        switch (typeId) {
            case BOOLEAN:
                return BOOLEAN_TYPE;
            case INTEGER:
                return INT_TYPE;
            case LONG:
                return LONG_TYPE;
            case FLOAT:
                return FLOAT_TYPE;
            case DOUBLE:
                return DOUBLE_TYPE;
            case DATE:
                return LOCAL_DATE_TYPE;
            case TIME:
                return LOCAL_TIME_TYPE;
            case TIMESTAMP:
                return LOCAL_DATE_TIME_TYPE;
            case STRING:
                return STRING_TYPE;
            case FIXED:
            case BINARY:
                return PrimitiveByteArrayType.INSTANCE;
            case DECIMAL:
                return new DecimalType(38, 18);
            case STRUCT:
            case LIST:
            case MAP:
            default:
                throw new IcebergConnectorException(
                        CommonErrorCode.UNSUPPORTED_DATA_TYPE,
                        String.format("Unsupported iceberg type: %s", typeId));
        }
    }

    @Override
    public String getIdentity() {
        return "Iceberg";
    }
}
