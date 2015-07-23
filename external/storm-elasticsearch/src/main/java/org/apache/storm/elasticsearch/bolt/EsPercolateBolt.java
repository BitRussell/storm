/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.storm.elasticsearch.bolt;

import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Tuple;
import backtype.storm.tuple.Values;
import org.apache.storm.elasticsearch.common.EsConfig;
import org.elasticsearch.action.percolate.PercolateResponse;
import org.elasticsearch.action.percolate.PercolateSourceBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class EsPercolateBolt extends AbstractEsBolt {
    private static final Logger LOG = LoggerFactory.getLogger(EsPercolateBolt.class);

    /**
     * EsPercolateBolt constructor
     * @param esConfig Elasticsearch configuration containing node addresses and cluster name {@link EsConfig}
     */
    public EsPercolateBolt(EsConfig esConfig) {
        super(esConfig);
    }

    @Override
    public void prepare(Map map, TopologyContext topologyContext, OutputCollector outputCollector) {
        super.prepare(map, topologyContext, outputCollector);
    }

    /**
     * Executes percolate request for given tuple.
     * @param tuple should contain string values of 3 declared fields: "source", "index", "type"
     */
    @Override
    public void execute(Tuple tuple) {
        try {
            String source = tuple.getStringByField("source");
            String index = tuple.getStringByField("index");
            String type = tuple.getStringByField("type");

            PercolateResponse response = client.preparePercolate().setIndices(index).setDocumentType(type)
                    .setPercolateDoc(PercolateSourceBuilder.docBuilder().setDoc(source)).execute().actionGet();
            if (response.getCount() > 0) {
                for (PercolateResponse.Match match : response) {
                    String id = match.getId().toString();
                    collector.emit(new Values(id));
                }
            }
            collector.ack(tuple);
        } catch (Exception e) {
            e.printStackTrace();
            collector.reportError(e);
            collector.fail(tuple);
        }
    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer outputFieldsDeclarer) {
        outputFieldsDeclarer.declare(new Fields("id"));
    }
}
