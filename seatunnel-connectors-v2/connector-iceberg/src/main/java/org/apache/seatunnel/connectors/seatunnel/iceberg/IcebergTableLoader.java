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

package org.apache.seatunnel.connectors.seatunnel.iceberg;

import org.apache.seatunnel.api.table.catalog.CatalogTable;
import org.apache.seatunnel.connectors.seatunnel.iceberg.config.CommonConfig;

import org.apache.commons.lang3.StringUtils;
import org.apache.iceberg.CachingCatalog;
import org.apache.iceberg.Table;
import org.apache.iceberg.catalog.Catalog;
import org.apache.iceberg.catalog.Namespace;
import org.apache.iceberg.catalog.TableIdentifier;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.io.Closeable;
import java.io.IOException;
import java.io.Serializable;

import static com.google.common.base.Preconditions.checkArgument;

@Slf4j
public class IcebergTableLoader implements Closeable, Serializable {

    private static final long serialVersionUID = 9061073826700804273L;

    private final IcebergCatalogFactory icebergCatalogFactory;
    private final String tableIdentifierStr;

    private Catalog catalog;

    public IcebergTableLoader(
            @NonNull IcebergCatalogFactory icebergCatalogFactory,
            @NonNull TableIdentifier tableIdentifier) {
        this.icebergCatalogFactory = icebergCatalogFactory;
        this.tableIdentifierStr = tableIdentifier.toString();
    }

    public void open() {
        catalog = CachingCatalog.wrap(icebergCatalogFactory.create());
    }

    public Table loadTable() {
        TableIdentifier tableIdentifier = TableIdentifier.parse(tableIdentifierStr);
        checkArgument(
                catalog.tableExists(tableIdentifier), "Illegal source table: " + tableIdentifier);
        return catalog.loadTable(tableIdentifier);
    }

    @Override
    public void close() throws IOException {
        if (catalog != null && catalog instanceof Closeable) {
            ((Closeable) catalog).close();
        }
    }

    public static IcebergTableLoader create(CommonConfig config) {
        IcebergCatalogFactory catalogFactory =
                new IcebergCatalogFactory(
                        config.getCatalogName(),
                        config.getCatalogType(),
                        config.getWarehouse(),
                        config.getUri(),
                        config.getKerberosPrincipal(),
                        config.getKerberosKrb5ConfPath(),
                        config.getKerberosKeytabPath(),
                        config.getHdfsSitePath(),
                        config.getHiveSitePath());
        return new IcebergTableLoader(
                catalogFactory,
                TableIdentifier.of(Namespace.of(config.getNamespace()), config.getTable()));
    }

    public static IcebergTableLoader create(CommonConfig config, CatalogTable catalogTable) {
        IcebergCatalogFactory catalogFactory =
                new IcebergCatalogFactory(
                        config.getCatalogName(),
                        config.getCatalogType(),
                        config.getWarehouse(),
                        config.getUri(),
                        config.getKerberosPrincipal(),
                        config.getKerberosKrb5ConfPath(),
                        config.getKerberosKeytabPath(),
                        config.getHdfsSitePath(),
                        config.getHiveSitePath());
        String table;
        if (catalogTable != null
                && StringUtils.isNotEmpty(catalogTable.getTableId().getTableName())) {
            log.info(
                    "conf table name is empty, use catalog table name: {}",
                    catalogTable.getTableId().getTableName());
            table = catalogTable.getTableId().getTableName();
        } else {
            throw new IllegalArgumentException("table name is empty");
        }

        return new IcebergTableLoader(
                catalogFactory, TableIdentifier.of(Namespace.of(config.getNamespace()), table));
    }
}
