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

package org.apache.seatunnel.connectors.seatunnel.iceberg.sink.writer;

import org.apache.seatunnel.api.table.type.SeaTunnelRow;
import org.apache.seatunnel.api.table.type.SeaTunnelRowType;

import org.apache.iceberg.FileFormat;
import org.apache.iceberg.PartitionKey;
import org.apache.iceberg.PartitionSpec;
import org.apache.iceberg.Schema;
import org.apache.iceberg.io.FileAppenderFactory;
import org.apache.iceberg.io.FileIO;
import org.apache.iceberg.io.OutputFileFactory;
import org.apache.iceberg.util.Tasks;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class PartitionedDeltaWriter extends BaseDeltaTaskWriter {

    private final PartitionKey partitionKey;

    private final Map<PartitionKey, RowDataDeltaWriter> writers = new HashMap<>();

    PartitionedDeltaWriter(
            PartitionSpec spec,
            FileFormat format,
            FileAppenderFactory<SeaTunnelRow> appenderFactory,
            OutputFileFactory fileFactory,
            FileIO io,
            long targetFileSize,
            Schema schema,
            SeaTunnelRowType seaTunnelSchema,
            List<Integer> equalityFieldIds,
            boolean upsert) {
        super(
                spec,
                format,
                appenderFactory,
                fileFactory,
                io,
                targetFileSize,
                schema,
                seaTunnelSchema,
                equalityFieldIds,
                upsert);
        this.partitionKey = new PartitionKey(spec, schema);
    }

    @Override
    RowDataDeltaWriter route(SeaTunnelRow row) {
        partitionKey.partition(wrapper().wrap(row));

        RowDataDeltaWriter writer = writers.get(partitionKey);
        if (writer == null) {
            // NOTICE: we need to copy a new partition key here, in case of messing up the keys in
            // writers.
            PartitionKey copiedKey = partitionKey.copy();
            writer = new RowDataDeltaWriter(copiedKey);
            writers.put(copiedKey, writer);
        }

        return writer;
    }

    @Override
    public void close() {
        try {
            Tasks.foreach(writers.values())
                    .throwFailureWhenFinished()
                    .noRetry()
                    .run(RowDataDeltaWriter::close, IOException.class);

            writers.clear();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to close equality delta writer", e);
        }
    }
}