/*
 * Jenkins cloud JUnit pipeline for MapsMessaging.
 *
 * Historical balancing source: Jenkins run #6944.
 * Two long-running suites are isolated:
 *   ServerTopicTest             ~48.4 minutes
 *   SimpleBufferBasedStompIT    ~23.7 minutes
 *
 * The remaining historically-known suites are greedily balanced into five
 * ~25 minute shards. Test classes added since that run are discovered from
 * src/test/java and deterministically distributed across the five shards.
 *
 * Intended Jenkins cloud label: ec2-fleet
 * Intended EC2 Fleet maximum: 4 agents
 */

def nodeLabel = "ec2-fleet"

def mavenCommon = [
    "-Dmaven.test.failure.ignore=true",
    "-Dpython_command=python3",
    "-DcomPort=/tmp/tty.modem",
    "-Dcom.datastax.driver.FORCE_NIO=true",
    "-Ddebug_domain=false",
    "-DfailIfNoTests=false",
    "-U"
].join(" ")

def heavySuites = [
    "io.mapsmessaging.network.protocol.impl.mqtt.ServerTopicTest",
    "io.mapsmessaging.network.protocol.impl.stomp.SimpleBufferBasedStompIT"
]

def historicalGroups = [

    [
        "io.mapsmessaging.network.protocol.impl.mqtt.MQTTWildCardSubscriptionImplTest",
        "io.mapsmessaging.network.protocol.impl.stomp.StompTransactionalPublishTest",
        "io.mapsmessaging.network.protocol.impl.mqtt_sn.MqttSNConnectionTest",
        "io.mapsmessaging.network.protocol.impl.mqtt_sn.MqttSNSubscriptionTest",
        "io.mapsmessaging.network.protocol.impl.mqtt5.ComplianceTests",
        "io.mapsmessaging.api.subscriptions.UnsubscribeValidationTest",
        "io.mapsmessaging.network.protocol.impl.n2k.N2kViaCanBusTest",
        "io.mapsmessaging.network.protocol.impl.mqtt_sn.MqttSnLargeMessageTest",
        "io.mapsmessaging.network.protocol.impl.amqp.jms.SimpleDurableConnectionTest",
        "io.mapsmessaging.network.protocol.impl.amqp.jms.BrowserConnectionTest",
        "io.mapsmessaging.network.protocol.impl.stomp.ExpiredEventTest",
        "io.mapsmessaging.network.protocol.impl.semtech.SemtechDownlinkViaUdpTest",
        "io.mapsmessaging.api.transactions.SimpleTransactionalTest",
        "io.mapsmessaging.rest.api.impl.discovery.DiscoveryManagementApiTest",
        "io.mapsmessaging.network.protocol.impl.mqtt_sn.MqttSnAuthTest",
        "io.mapsmessaging.network.protocol.impl.amqp.jms.messages.ObjectMessageTypeTest",
        "io.mapsmessaging.network.protocol.impl.nats.jetstream.JetStreamConsumerTest",
        "io.mapsmessaging.network.protocol.impl.amqp.jms.messages.BaseMessageTypeTest",
        "io.mapsmessaging.network.protocol.impl.amqp.jms.messages.MapMessageTypeTest",
        "io.mapsmessaging.rest.api.impl.auth.LockedUsersApiTest",
        "io.mapsmessaging.rest.api.impl.server.CacheManagementApiTest",
        "io.mapsmessaging.network.io.security.PacketIntegrityVerificationTests",
        "io.mapsmessaging.utilities.stats.processors.AverageDataProcessorTest",
        "io.mapsmessaging.engine.destination.MessageOverridesTest",
        "io.mapsmessaging.api.queue.QueueTest",
        "io.mapsmessaging.dto.rest.config.transformer.TransformationConfigDtoGsonPolymorphismTest",
        "io.mapsmessaging.rest.api.impl.destination.NamespaceTreeRestPagingSemanticsTest",
        "io.mapsmessaging.aggregator.mailbox.QueueBackedMpscMailboxTest",
        "io.mapsmessaging.utilities.stats.MovingAverageFactoryTest",
        "io.mapsmessaging.engine.destination.subscription.DestinationSubscriptionManagerTest",
        "io.mapsmessaging.network.protocol.impl.mavlink.UpdateTest",
        "io.mapsmessaging.network.protocol.impl.echo.EchoConnectionTest",
        "io.mapsmessaging.license.tools.LicenseFetcherTest",
        "io.mapsmessaging.network.protocol.impl.proxy.ProxyProtocolV1SpecEnforcementTest",
        "io.mapsmessaging.network.io.impl.noop.NoOpEndPointTest",
        "io.mapsmessaging.network.io.impl.noop.NoOpEndPointServerTest",
        "io.mapsmessaging.network.protocol.impl.proxy.ProxyProtocolV2SpecEnforcementTest",
        "io.mapsmessaging.api.MessageBuilderTest",
        "io.mapsmessaging.network.io.impl.noop.NoOpEndPointServerFactoryTest",
        "io.mapsmessaging.analytics.impl.stats.AdvancedStatisticsTest",
        "io.mapsmessaging.api.message.MessagePackRoundTripTest",
        "io.mapsmessaging.network.protocol.impl.mqtt.WildcardTest",
        "io.mapsmessaging.network.protocol.impl.stomp.StompTransactionalSubscriptionImplTest"
    ], // historical total ~25.1 min
    [
        "io.mapsmessaging.network.protocol.impl.mqtt5.SimpleOverlapTest",
        "io.mapsmessaging.network.protocol.impl.mqtt5.MQTTPublishEventTest",
        "io.mapsmessaging.network.protocol.impl.mqtt5.MQTTConnectionTest",
        "io.mapsmessaging.utilities.stats.SimpleStatsTest",
        "io.mapsmessaging.network.protocol.impl.conformance.PahoMQTT3ConformanceIT",
        "io.mapsmessaging.network.protocol.impl.nats.conv.NatsPingBehaviorTest",
        "io.mapsmessaging.network.protocol.impl.mqtt_sn.packet.UDPConnectPacketTest",
        "io.mapsmessaging.api.SessionTest",
        "io.mapsmessaging.network.protocol.impl.coap.CoapObserverTest",
        "io.mapsmessaging.rest.cache.CacheTest",
        "io.mapsmessaging.network.protocol.impl.amqp.jms.TemporaryDestinationTest",
        "io.mapsmessaging.utilities.stats.MovingAverageTest",
        "io.mapsmessaging.network.protocol.impl.stomp.WebSocketTest",
        "io.mapsmessaging.utilities.stats.LinkedMovingAveragesTest",
        "io.mapsmessaging.rest.api.impl.auth.UserManagementApiTest",
        "io.mapsmessaging.rest.api.impl.connections.ConnectionManagementApiTest",
        "io.mapsmessaging.rest.api.impl.hardware.HardwareManagementApiTest",
        "io.mapsmessaging.rest.api.impl.interfaces.InterfaceInstanceApiTest",
        "io.mapsmessaging.rest.api.impl.logging.LogMonitorRestApiTest",
        "io.mapsmessaging.network.protocol.impl.semtech.SemtechViaUdpRigidTest",
        "io.mapsmessaging.network.protocol.impl.nats.NatsControlFramesTest",
        "io.mapsmessaging.rest.api.impl.destination.NamespaceTreeRealisticAbsoluteHierarchyWalkTest",
        "io.mapsmessaging.api.transformers.GeoHashResolverTest",
        "io.mapsmessaging.network.protocol.impl.nats.jetstream.JetStreamStreamTest",
        'io.mapsmessaging.utilities.GeoHashUtilsTest$ValidationFailures',
        "io.mapsmessaging.engine.destination.subscription.state.MessageStateManagerImplTest",
        "io.mapsmessaging.rest.api.impl.destination.NamespaceTreePagingLargeTest",
        "io.mapsmessaging.network.protocol.impl.nats.conv.NatsProtocolBasicsTest",
        "io.mapsmessaging.engine.destination.delayed.MessageManagerTest",
        "io.mapsmessaging.api.transformers.JsonQueryTransformationTest",
        "io.mapsmessaging.network.protocol.impl.satellite.SatelliteMessageFactoryRebuilderTest",
        "io.mapsmessaging.network.protocol.impl.satellite.PackingPipelineTest",
        "io.mapsmessaging.utilities.stats.processors.DifferenceDataProcessorTest",
        "io.mapsmessaging.network.protocol.impl.nats.jetstream.JetStreamInfoTest",
        "io.mapsmessaging.dto.rest.config.transformer.jsonmutate.JsonMutatorTest",
        "io.mapsmessaging.engine.destination.subscription.transaction.CreditManagerTest",
        "io.mapsmessaging.engine.destination.subscription.impl.shared.SharedSubscriptionRegisterTest",
        "io.mapsmessaging.network.protocol.impl.mavlink.MavlinkStreamHandlerTest",
        "io.mapsmessaging.network.io.security.PacketIntegrityFactoryTests",
        "io.mapsmessaging.network.protocol.impl.proxy.ProxyProtocolV1Test",
        "io.mapsmessaging.network.protocol.impl.satellite.protocol.SatelliteGatewayHardeningTest",
        "io.mapsmessaging.analytics.impl.stats.QualityStatisticsTest"
    ], // historical total ~25.1 min
    [
        "io.mapsmessaging.network.protocol.impl.mqtt.SimpleOverlapTest",
        "io.mapsmessaging.network.protocol.impl.mqtt5.QueueSubscriptionTest",
        "io.mapsmessaging.network.protocol.impl.mqtt5.SubscriptionTest",
        "io.mapsmessaging.network.protocol.impl.mqtt_sn.MqttSnSleepTest",
        "io.mapsmessaging.network.protocol.impl.mqtt.SimpleBufferBasedMQTTIT",
        "io.mapsmessaging.network.protocol.impl.mqtt5.SimpleBufferBasedMQTT5IT",
        "io.mapsmessaging.network.protocol.impl.amqp.jms.SimpleTransactionConnectionTest",
        "io.mapsmessaging.network.protocol.impl.mqtt_sn.packet.HmacConnectPacketTest",
        "io.mapsmessaging.api.subscriptions.FilteredQueueSubscriptionTest",
        "io.mapsmessaging.network.protocol.impl.mqtt_sn.MqttSNPublishingTest",
        "io.mapsmessaging.api.subscriptions.SelectorTest",
        "io.mapsmessaging.rest.api.impl.config.ConfigManagementApiTest",
        "io.mapsmessaging.rest.cache.impl.RoleBasedCacheTest",
        "io.mapsmessaging.network.protocol.impl.coap.BlockwiseReceiveTest",
        "io.mapsmessaging.rest.api.impl.ml.ModelStoreApiTest",
        "io.mapsmessaging.network.protocol.impl.nats.NatsPubSubTest",
        "io.mapsmessaging.tools.config.schema.tests.JsonSchemaRoundTripTest",
        "io.mapsmessaging.rest.api.impl.messaging.MessagingApiTest",
        "io.mapsmessaging.rest.api.impl.server.ServerHealthApiTest",
        "io.mapsmessaging.rest.api.impl.auth.AuthorisationResourceTest",
        "io.mapsmessaging.api.transformers.ProtobufSchemaToJsonTest",
        "io.mapsmessaging.network.protocol.impl.amqp.jms.messages.TextMessageTypeTest",
        "io.mapsmessaging.engine.destination.subscription.transaction.AutoAcknowledgementControllerTest",
        "io.mapsmessaging.engine.destination.subscription.impl.shared.SharedSubscriptionManagerTest",
        "io.mapsmessaging.api.transformers.AvroSchemaToJsonTest",
        "io.mapsmessaging.api.transformers.XMLToJSONTest",
        "io.mapsmessaging.network.protocol.impl.nats.NatsEdgeCasesTest",
        "io.mapsmessaging.aggregator.worker.StaticAggregatorWorkSchedulerTest",
        "io.mapsmessaging.engine.destination.RetainManagerTest",
        "io.mapsmessaging.analytics.impl.stats.StatisticsTests",
        "io.mapsmessaging.engine.destination.subscription.state.BoundedMessageStateManagerTest",
        "io.mapsmessaging.api.transformers.JSONToXMLTest",
        "io.mapsmessaging.api.transformers.TopicNameComputerTest",
        "io.mapsmessaging.network.protocol.impl.mavlink.GsonFactoryTest",
        "io.mapsmessaging.utilities.stats.processors.AdderDataProcessorTest",
        "io.mapsmessaging.api.transformers.CloudEventNativeTransformationTest",
        "io.mapsmessaging.engine.destination.delayed.DelayedBucketTest",
        "io.mapsmessaging.analytics.impl.stats.TimeWindowMovingAverageTest",
        "io.mapsmessaging.analytics.impl.stats.QuantileStatisticsTest",
        "io.mapsmessaging.api.transformers.JsonMutateTransformationTest",
        "io.mapsmessaging.analytics.impl.stats.MovingAverageStatisticsTest",
        "io.mapsmessaging.analytics.impl.stats.MomentStatisticsReferenceTest",
        "io.mapsmessaging.network.protocol.impl.echo.EchoSSLPublishMessagesTest",
        "io.mapsmessaging.network.protocol.impl.echo.EchoSSLConnectionTest"
    ], // historical total ~25.1 min
    [
        "io.mapsmessaging.network.protocol.impl.mqtt5.SystemTopicTest",
        "io.mapsmessaging.network.protocol.impl.mqtt5.MQTTStoredMessageTest",
        "io.mapsmessaging.network.protocol.impl.mqtt.QueueSubscriptionTest",
        "io.mapsmessaging.network.protocol.impl.stomp.ExtendedSelectorTest",
        "io.mapsmessaging.network.protocol.impl.satellite.SatelliteReplicationReverseTests",
        "io.mapsmessaging.network.protocol.impl.mqtt.MQTTStoredMessageTest",
        "io.mapsmessaging.network.protocol.impl.stomp.StompPublishEventTest",
        "io.mapsmessaging.network.protocol.impl.mqtt.MQTTConnectionTest",
        "io.mapsmessaging.network.protocol.impl.mqtt.MQTTPublishEventTest",
        "io.mapsmessaging.network.protocol.impl.amqp.jms.SimpleConnectionTest",
        "io.mapsmessaging.aggregator.StaticAggregatorSystemTest",
        "io.mapsmessaging.network.protocol.impl.satellite.PackingTest",
        "io.mapsmessaging.network.protocol.impl.amqp.jms.messages.BytesMessageTypeTest",
        "io.mapsmessaging.rest.destination.DestinationListManagementApiTest",
        "io.mapsmessaging.rest.api.impl.integration.IntegrationInstanceManagementApiTest",
        "io.mapsmessaging.network.protocol.impl.nats.NatsVerbTest",
        "io.mapsmessaging.rest.api.impl.integration.IntegrationManagementApiTest",
        "io.mapsmessaging.rest.api.impl.auth.GroupManagementApiTest",
        "io.mapsmessaging.api.overrides.BaseOverridesTest",
        "io.mapsmessaging.network.route.LinkSelectorEdgeCasesTest",
        "io.mapsmessaging.network.protocol.impl.amqp.jms.messages.StreamMessageTypeTest",
        "io.mapsmessaging.rest.api.impl.server.ServerDetailsApiTest",
        "io.mapsmessaging.network.protocol.impl.mavlink.MavlinkViaUdpTest",
        "io.mapsmessaging.aggregator.StaticAggregatorTransformationSystemTest",
        "io.mapsmessaging.network.route.LinkSelectorDecisionTest",
        "io.mapsmessaging.network.protocol.impl.nats.NatsJetStreamLikeTest",
        "io.mapsmessaging.network.protocol.impl.semtech.SemtechViaUdpTest",
        'io.mapsmessaging.utilities.GeoHashUtilsTest$Concurrency',
        "io.mapsmessaging.state.drone.TwinManagerTest",
        "io.mapsmessaging.engine.destination.subscription.set.DestinationSetTest",
        "io.mapsmessaging.api.transformers.JsonToValueTransformationTest",
        "io.mapsmessaging.api.transformers.CloudEventJsonTransformationTest",
        'io.mapsmessaging.utilities.GeoHashUtilsTest$HappyPath',
        "io.mapsmessaging.api.transformers.jsonmapper.JsonMapFunctionsTest",
        "io.mapsmessaging.engine.destination.subscription.state.LimitedMessageStateManagerTest",
        "io.mapsmessaging.network.protocol.impl.local.LoopbackEngineExecutorTest",
        "io.mapsmessaging.engine.destination.subscription.SubscriptionContextTest",
        "io.mapsmessaging.network.protocol.impl.proxy.ProxyProtocolV2Test",
        "io.mapsmessaging.dto.rest.config.transformer.impl.GeoHashResolverTransformationDtoDefaultsTest",
        "io.mapsmessaging.analytics.impl.stats.WindowedStatisticsTest",
        "io.mapsmessaging.analytics.impl.stats.StringStatisticsTest",
        "io.mapsmessaging.analytics.impl.stats.MomentStatisticsTest",
        "io.mapsmessaging.network.protocol.impl.proxy.ProxyProtocolV2LengthLimitTest",
        "io.mapsmessaging.network.protocol.impl.stomp.StompQueueTest"
    ], // historical total ~25.1 min
    [
        "io.mapsmessaging.network.protocol.impl.mqtt5.MqttAuthSaslTest",
        "io.mapsmessaging.network.protocol.impl.conformance.PahoMQTT5ConformanceIT",
        "io.mapsmessaging.network.protocol.impl.mqtt5.MQTTSubscriptionImplTest",
        "io.mapsmessaging.network.protocol.impl.satellite.SatelliteReplicationTests",
        "io.mapsmessaging.network.protocol.impl.mqtt.MQTTSubscriptionImplTest",
        "io.mapsmessaging.network.protocol.impl.nats.conv.HighFanoutOrderingTest",
        "io.mapsmessaging.rest.SmokeTest",
        "io.mapsmessaging.network.protocol.impl.mqtt_sn.packet.DTLSConnectPacketTest",
        "io.mapsmessaging.network.protocol.impl.amqp.client.StandardClientTest",
        "io.mapsmessaging.network.protocol.impl.canaerospace.CanAerospaceViaCanBusTest",
        "io.mapsmessaging.network.protocol.impl.mavlink.MavlinkStreamHandlerHangTest",
        "io.mapsmessaging.api.subscriptions.DelayedPublishTest",
        "io.mapsmessaging.network.protocol.impl.coap.CoapSimpleInteractionTest",
        "io.mapsmessaging.ha.FileLockManagerTest",
        "io.mapsmessaging.network.protocol.impl.amqp.jms.FilteredSubscriptionTest",
        "io.mapsmessaging.network.protocol.impl.stomp.StompConnectionTest",
        "io.mapsmessaging.rest.api.impl.schema.SchemaQueryApiTest",
        "io.mapsmessaging.aggregator.StaticAggregatorOutboundTransformationSystemTest",
        "io.mapsmessaging.engine.destination.subscription.SubscriptionBuilderTest",
        "io.mapsmessaging.rest.api.impl.interfaces.InterfaceManagementApiTest",
        "io.mapsmessaging.utilities.GeoHashUtilsTest",
        "io.mapsmessaging.rest.api.impl.lora.LoRaDeviceApiTest",
        "io.mapsmessaging.license.LicenseServerClientTest",
        "io.mapsmessaging.api.transformers.InterServerTransformationPipelineTest",
        "io.mapsmessaging.network.protocol.impl.coap.BlockwiseSendTest",
        "io.mapsmessaging.engine.destination.subscription.state.IteratorStateManagerImplTest",
        "io.mapsmessaging.network.io.security.PacketIntegritySecurityTests",
        "io.mapsmessaging.network.protocol.impl.nats.NatsHeadersTest",
        "io.mapsmessaging.utilities.filtering.NamespaceFiltersTest",
        "io.mapsmessaging.api.transformers.jsonmapper.JsonMapperTest",
        "io.mapsmessaging.network.io.security.PacketValidationTests",
        "io.mapsmessaging.api.transformers.CloudEventEnvelopeTransformationTest",
        "io.mapsmessaging.rest.api.impl.destination.NamespaceTreeTest",
        "io.mapsmessaging.engine.destination.subscription.set.DestinationSetUsageTest",
        "io.mapsmessaging.network.EndPointURLTest",
        "io.mapsmessaging.network.io.impl.noop.NoOpEndPointConnectionFactoryTest",
        "io.mapsmessaging.network.protocol.impl.echo.EchoPublishMessagesTest",
        "io.mapsmessaging.api.message.MessageFactoryTest",
        "io.mapsmessaging.network.protocol.impl.proxy.ProxyProtocolFragmentedDetectionTest",
        "io.mapsmessaging.analytics.impl.stats.BaseStatisticsTest",
        "io.mapsmessaging.analytics.impl.stats.TrendStatisticsTest",
        "io.mapsmessaging.network.protocol.impl.satellite.protocol.SatelliteMessageRebuilderTest",
        "io.mapsmessaging.network.protocol.impl.mqtt5.packet.properties.MessagePropertiesTest"
    ] // historical total ~25.1 min

]

def knownTests = (heavySuites + historicalGroups.flatten()).toSet()

def discoverNewTestsForShard = { int shardIndex ->
    def discovered = sh(
        script: '''
            find src/test/java -type f |
            grep -E '/[^/]+(Test|Tests|TestCase|IT)[.]java$' |
            sed -e 's#^src/test/java/##' -e 's#/#.#g' -e 's#[.]java$##' |
            sort -u
        ''',
        returnStdout: true
    ).trim()

    if (!discovered) {
        return []
    }

    discovered.readLines().findAll { className ->
        !knownTests.contains(className) &&
        Math.floorMod(className.hashCode(), 5) == shardIndex
    }
}

def runSuite = { String stageName, List<String> tests, Integer dynamicShardIndex ->
    node(nodeLabel) {
        stage(stageName) {
            deleteDir()
            checkout scm

            def selectedTests = new ArrayList<String>(tests)
            if (dynamicShardIndex != null) {
                def newTests = discoverNewTestsForShard(dynamicShardIndex)
                if (newTests) {
                    echo "${stageName}: adding ${newTests.size()} test classes not present in the historical timing data"
                    selectedTests.addAll(newTests)
                }
            }

            if (selectedTests.isEmpty()) {
                error "${stageName}: no tests selected"
            }

            echo "${stageName}: running ${selectedTests.size()} test classes"

            def selector = selectedTests.join(",")
            sh """#!/bin/bash
                set -euo pipefail
                mvn clean test -Dtest='${selector}' ${mavenCommon}
            """
        }

        stage("${stageName} - JUnit") {
            junit(
                allowEmptyResults: false,
                testResults: "**/target/surefire-reports/*.xml"
            )
        }

        stage("${stageName} - Coverage Data") {
            def coverageId = stageName.replaceAll("[^A-Za-z0-9_.-]", "_")
            sh """#!/bin/bash
                set -euo pipefail
                mkdir -p coverage
                cp target/jacoco.exec "coverage/${coverageId}.exec"
            """
            stash(
                name: "jacoco-${coverageId}",
                includes: "coverage/${coverageId}.exec"
            )
        }
    }
}

def branches = [:]

branches["Long - ServerTopicTest"] = {
    runSuite("Long - ServerTopicTest", [heavySuites[0]], null)
}

branches["Long - SimpleBufferBasedStompIT"] = {
    runSuite("Long - SimpleBufferBasedStompIT", [heavySuites[1]], null)
}

branches["JUnit shard 1"] = {
    runSuite("JUnit shard 1", historicalGroups[0], 0)
}

branches["JUnit shard 2"] = {
    runSuite("JUnit shard 2", historicalGroups[1], 1)
}

branches["JUnit shard 3"] = {
    runSuite("JUnit shard 3", historicalGroups[2], 2)
}

branches["JUnit shard 4"] = {
    runSuite("JUnit shard 4", historicalGroups[3], 3)
}

branches["JUnit shard 5"] = {
    runSuite("JUnit shard 5", historicalGroups[4], 4)
}

parallel branches

node(nodeLabel) {
    stage("Coverage") {
        deleteDir()
        checkout scm

        def coverageStageNames = [
            "Long - ServerTopicTest",
            "Long - SimpleBufferBasedStompIT",
            "JUnit shard 1",
            "JUnit shard 2",
            "JUnit shard 3",
            "JUnit shard 4",
            "JUnit shard 5"
        ]

        coverageStageNames.each { String coverageStageName ->
            def coverageId = coverageStageName.replaceAll("[^A-Za-z0-9_.-]", "_")
            unstash "jacoco-${coverageId}"
        }

        sh '''#!/bin/bash
            set -euo pipefail

            mvn -q -Dtransitive=false dependency:get \
              -Dartifact=org.jacoco:org.jacoco.cli:0.8.15:jar:nodeps

            mkdir -p target
            java -jar "$HOME/.m2/repository/org/jacoco/org.jacoco.cli/0.8.15/org.jacoco.cli-0.8.15-nodeps.jar" \
              merge coverage/*.exec \
              --destfile target/jacoco.exec

            mvn -DskipTests compile jacoco:report \
              -Djacoco.dataFile=target/jacoco.exec
        '''

        recordCoverage(
            tools: [[
                parser: "JACOCO",
                pattern: "target/site/jacoco/jacoco.xml"
            ]],
            id: "jacoco",
            name: "JaCoCo Coverage",
            sourceCodeRetention: "EVERY_BUILD"
        )

        archiveArtifacts(
            artifacts: "target/site/jacoco/jacoco.xml",
            fingerprint: true
        )
    }

    stage("SonarCloud") {
        withCredentials([
            string(credentialsId: "sonarcloud-token", variable: "SONAR_TOKEN")
        ]) {
            sh '''#!/bin/bash
                set -euo pipefail

                mvn -DskipTests sonar:sonar \
                  -Dsonar.token="$SONAR_TOKEN"
            '''
        }
    }
}

stage("Summary") {
    echo "JUnit cloud run complete"
}
