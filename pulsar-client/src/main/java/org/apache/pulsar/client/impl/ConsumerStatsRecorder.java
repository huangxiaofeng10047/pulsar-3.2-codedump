/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.pulsar.client.impl;

import io.netty.util.Timeout;
import java.util.Optional;
import org.apache.pulsar.client.api.ConsumerStats;
import org.apache.pulsar.client.api.Message;
import org.apache.pulsar.client.api.ProducerStats;

public interface ConsumerStatsRecorder extends ConsumerStats {
    void updateNumMsgsReceived(Message<?> message);

    void incrementNumAcksSent(long numAcks);

    void incrementNumAcksFailed();

    void incrementNumReceiveFailed();

    void incrementNumBatchReceiveFailed();

    Optional<Timeout> getStatTimeout();

    void reset();

    void updateCumulativeStats(ConsumerStats stats);

    void setDeadLetterProducerStats(ProducerStats producerStats);

    void setRetryLetterProducerStats(ProducerStats producerStats);
}
