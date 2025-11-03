pipeline {
    agent any

    tools {
        maven 'MavenLocal'   // Jenkins -> Global Tool Config -> Maven name
    }

    options {
        timestamps()
    }

    stages {

        stage('Checkout Code') {
            steps {
                checkout scm
            }
        }

        stage('Build & Run Tests') {
            steps {
                script {
                    if (isUnix()) {
                        sh 'mvn clean test -Dsurefire.suiteXmlFiles=testng.xml'
                    } else {
                        bat 'mvn clean test -Dsurefire.suiteXmlFiles=testng.xml'
                    }
                }
            }
        }

        stage('Publish Extent Report') {
            steps {
                publishHTML([
                    reportDir: 'test-output',              // folder where Extent report exists
                    reportFiles: 'ExtentReport.html',      // exact report file name
                    reportName: 'Extent Report',
                    keepAll: true,
                    alwaysLinkToLastBuild: true,
                    allowMissing: true
                ])
            }
        }

        stage('Publish Reports') {
            steps {
                // TestNG results
                testNG reportFilenamePattern: '**/test-output/testng-results.xml',
                      escapeTestDescp: true, escapeExceptionMsg: true

                // JUnit XML
                junit allowEmptyResults: true, testResults: '**/surefire-reports/*.xml'

                // Archive results for download
                archiveArtifacts artifacts: 'test-output/**, target/**', fingerprint: true
            }
        }
    }

    post {
        success {
            echo "✅ Tests passed successfully!"
        }
        failure {
            echo "❌ Test Execution Failed!"
        }
        always {
            echo "📦 Build completed: ${env.BUILD_URL}"
        }
    }
}
