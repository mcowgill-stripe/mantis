/*
 * Copyright 2024 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.mantisrx.extensions.dynamodb;

import com.amazonaws.services.dynamodbv2.AcquireLockOptions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBLockClient;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBLockClientOptions;
import com.amazonaws.services.dynamodbv2.LockItem;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.junit.rules.ExternalResource;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement;

public class DynamoDBLockSupportRule extends ExternalResource {
    private final String tableName;
    private final DynamoDbClient dbClient;

    private final AmazonDynamoDBLockClient lockClient;

    public DynamoDBLockSupportRule(String tableName, DynamoDbClient dbClient) {
        this.tableName = tableName;
        this.dbClient = dbClient;
        this.lockClient =
                new AmazonDynamoDBLockClient(
                        AmazonDynamoDBLockClientOptions.builder(this.dbClient, this.tableName)
                                .withLeaseDuration(6L)
                                .withHeartbeatPeriod(2L)
                                .withCreateHeartbeatBackgroundThread(true)
                                .withTimeUnit(TimeUnit.SECONDS)
                                .build());
    }

    @Override
    protected void before() {
        this.dbClient.createTable(
                ctb ->
                        ctb.tableName(this.tableName)
                                .keySchema(KeySchemaElement.builder().attributeName("key").keyType("HASH").build())
                                .attributeDefinitions(
                                        AttributeDefinition.builder().attributeName("key").attributeType("S").build())
                                .provisionedThroughput(ptb -> ptb.readCapacityUnits(300L).writeCapacityUnits(300L))
                                .build());
    }

    @Override
    protected void after() {
        try {
            this.lockClient.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        this.dbClient.deleteTable(dtb -> dtb.tableName(this.tableName));
    }

    public LockItem takeLock(String lockKey, byte[] data) throws InterruptedException {
        return this.lockClient.acquireLock(
                AcquireLockOptions.builder(lockKey)
                        .withReplaceData(true)
                        .withShouldSkipBlockingWait(true)
                        .withAcquireReleasedLocksConsistently(true)
                        .withData(ByteBuffer.wrap(data))
                        .build());
    }

    public void releaseLock(String lockKey) {
        final Optional<LockItem> lockItem = this.lockClient.getLock(lockKey, Optional.empty());
        lockItem.ifPresent(LockItem::close);
    }
}
