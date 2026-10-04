/*
 * Jenkins cloud JUnit pipeline for MapsMessaging.
 *
 * Tests are partitioned into deterministic subsystem/package work units.
 * Long-running ServerTopicTest and SimpleBufferBasedStompIT remain isolated.
 * Every work unit is submitted in parallel and queues for the next available
 * ec2-fleet executor. Unclassified test classes run in JUnit - Misc.
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

def blockFor = { String className ->
    if (className == "io.mapsmessaging.network.protocol.impl.mqtt.ServerTopicTest") {
        return "Long - ServerTopicTest"
    }
    if (className == "io.mapsmessaging.network.protocol.impl.stomp.SimpleBufferBasedStompIT") {
        return "Long - SimpleBufferBasedStompIT"
    }

    if (className.startsWith("io.mapsmessaging.network.protocol.impl.mqtt5.")) {
        return "JUnit - MQTT5"
    }
    if (className.startsWith("io.mapsmessaging.network.protocol.impl.mqtt_sn.")) {
        return "JUnit - MQTT-SN"
    }
    if (className.startsWith("io.mapsmessaging.network.protocol.impl.mqtt.")) {
        return "JUnit - MQTT"
    }
    if (className.startsWith("io.mapsmessaging.network.protocol.impl.stomp.")) {
        return "JUnit - STOMP"
    }
    if (className.startsWith("io.mapsmessaging.network.protocol.impl.amqp.")) {
        return "JUnit - AMQP"
    }
    if (className.startsWith("io.mapsmessaging.network.protocol.impl.nats.")) {
        return "JUnit - NATS"
    }
    if (className.startsWith("io.mapsmessaging.network.protocol.impl.coap.")) {
        return "JUnit - CoAP"
    }
    if (className.startsWith("io.mapsmessaging.network.protocol.impl.n2k.") ||
        className.startsWith("io.mapsmessaging.network.protocol.impl.canaerospace.") ||
        className.startsWith("io.mapsmessaging.network.io.impl.canbus.")) {
        return "JUnit - CAN-N2K"
    }
    if (className.startsWith("io.mapsmessaging.network.protocol.impl.mavlink.")) {
        return "JUnit - MAVLink"
    }
    if (className.startsWith("io.mapsmessaging.network.protocol.")) {
        return "JUnit - Other Protocols"
    }

    if (className.startsWith("io.mapsmessaging.rest.")) {
        return "JUnit - REST"
    }
    if (className.startsWith("io.mapsmessaging.engine.destination.subscription.")) {
        return "JUnit - Subscription Engine"
    }
    if (className.startsWith("io.mapsmessaging.engine.destination.")) {
        return "JUnit - Destination Engine"
    }
    if (className.startsWith("io.mapsmessaging.api.transformers.")) {
        return "JUnit - Transformers"
    }
    if (className.startsWith("io.mapsmessaging.api.")) {
        return "JUnit - API"
    }
    if (className.startsWith("io.mapsmessaging.aggregator.") ||
        className.startsWith("io.mapsmessaging.analytics.")) {
        return "JUnit - Aggregator-Analytics"
    }
    if (className.startsWith("io.mapsmessaging.network.monitor.")) {
        return "JUnit - Network Monitor"
    }
    if (className.startsWith("io.mapsmessaging.network.discovery.")) {
        return "JUnit - Discovery"
    }
    if (className.startsWith("io.mapsmessaging.network.io.") ||
        className.startsWith("io.mapsmessaging.network.route.") ||
        className == "io.mapsmessaging.network.EndPointURLTest") {
        return "JUnit - Network Core"
    }
    if (className.startsWith("io.mapsmessaging.state.") ||
        className.startsWith("io.mapsmessaging.ha.") ||
        className.startsWith("io.mapsmessaging.license.")) {
        return "JUnit - State-HA-License"
    }
    if (className.startsWith("io.mapsmessaging.utilities.") ||
        className.startsWith("io.mapsmessaging.configuration.") ||
        className.startsWith("io.mapsmessaging.dto.") ||
        className.startsWith("io.mapsmessaging.tools.")) {
        return "JUnit - Utilities-Config"
    }

    return "JUnit - Misc"
}

def discoverTests = {
    def discovered = sh(
        script: '''
            find src/test/java -type f |
            grep -E '/[^/]+(Test|Tests|TestCase|IT)[.]java$' |
            sed -e 's#^src/test/java/##' -e 's#/#.#g' -e 's#[.]java$##' |
            sort -u
        ''',
        returnStdout: true
    ).trim()

    discovered ? discovered.readLines() : []
}

def runBlock = { String stageName ->
    node(nodeLabel) {
        stage(stageName) {
            deleteDir()
            checkout scm

            def selectedTests = discoverTests().findAll { className ->
                blockFor(className) == stageName
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

def blockNames = [
    "Long - ServerTopicTest",
    "Long - SimpleBufferBasedStompIT",
    "JUnit - MQTT",
    "JUnit - MQTT5",
    "JUnit - MQTT-SN",
    "JUnit - STOMP",
    "JUnit - AMQP",
    "JUnit - NATS",
    "JUnit - CoAP",
    "JUnit - CAN-N2K",
    "JUnit - MAVLink",
    "JUnit - Other Protocols",
    "JUnit - REST",
    "JUnit - Destination Engine",
    "JUnit - Subscription Engine",
    "JUnit - API",
    "JUnit - Transformers",
    "JUnit - Aggregator-Analytics",
    "JUnit - Network Monitor",
    "JUnit - Discovery",
    "JUnit - Network Core",
    "JUnit - State-HA-License",
    "JUnit - Utilities-Config",
    "JUnit - Misc"
]

def branches = [:]

blockNames.each { String blockName ->
    def currentBlock = blockName
    branches[currentBlock] = {
        runBlock(currentBlock)
    }
}

timeout(time: 2, unit: "HOURS") {
    parallel branches
}

node(nodeLabel) {
    stage("Coverage") {
        deleteDir()
        checkout scm

        def coverageStageNames = blockNames

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

            mvn -DskipTests test-compile jacoco:report \
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
                  -Dsonar.branch.name=development \
                  -Dsonar.token="$SONAR_TOKEN"
            '''
        }
    }
}

stage("Summary") {
    echo "JUnit cloud run complete"
}
