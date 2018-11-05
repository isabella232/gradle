/*
 * Copyright 2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.scala.compile

import org.gradle.integtests.fixtures.AbstractIntegrationSpec
import org.gradle.integtests.fixtures.AvailableJavaHomes
import org.gradle.util.Requires
import org.gradle.util.TestPrecondition
import spock.lang.Unroll

import static org.gradle.api.JavaVersion.VERSION_1_7
import static org.gradle.api.JavaVersion.VERSION_1_8

@Unroll
class UpToDateScalaCompileIntegrationTest extends AbstractIntegrationSpec {

    def setup() {
        file('src/main/scala/Person.scala') << "class Person(name: String)"
    }

    @Requires(TestPrecondition.JDK8_OR_EARLIER)
    def "compile is out of date when changing the #changedVersion version"() {
        buildScript(scalaProjectBuildScript(defaultZincVersion, defaultScalaVersion))

        when:
        run 'compileScala'

        then:
        executedAndNotSkipped ':compileScala'

        when:
        run 'compileScala'

        then:
        skipped ':compileScala'

        when:
        buildScript(scalaProjectBuildScript(newZincVersion, newScalaVersion))
        run 'compileScala'

        then:
        executedAndNotSkipped ':compileScala'

        where:
        newScalaVersion | newZincVersion
        '2.11.8'       | '1.2.4'
        '2.11.9'        | '1.2.5'
        defaultScalaVersion = '2.11.8'
        defaultZincVersion = '1.2.5'
        changedVersion = defaultScalaVersion != newScalaVersion ? 'scala' : 'zinc'
    }

    @Requires(adhoc = { AvailableJavaHomes.getJdk(VERSION_1_7) && AvailableJavaHomes.getJdk(VERSION_1_8) })
    def "compile is out of date when changing the java version"() {
        def jdk7 = AvailableJavaHomes.getJdk(VERSION_1_7)
        def jdk8 = AvailableJavaHomes.getJdk(VERSION_1_8)

        buildScript(scalaProjectBuildScript('1.2.5', '2.11.8'))
        when:
        executer.withJavaHome(jdk7.javaHome)
        run 'compileScala'

        then:
        executedAndNotSkipped(':compileScala')

        when:
        executer.withJavaHome(jdk7.javaHome)
        run 'compileScala'
        then:
        skipped ':compileScala'

        when:
        executer.withJavaHome(jdk8.javaHome)
        run 'compileScala', '--info'
        then:
        executedAndNotSkipped(':compileScala')
    }

    def scalaProjectBuildScript(String zincVersion, String scalaVersion) {
        return """
            apply plugin: 'scala'
                        
            ${jcenterRepository()}

            dependencies {
                zinc "org.scala-sbt:zinc_2.12:${zincVersion}"
                compile "org.scala-lang:scala-library:${scalaVersion}" 
            }
            
            sourceCompatibility = '1.7'
            targetCompatibility = '1.7'
        """.stripIndent()
    }

}
