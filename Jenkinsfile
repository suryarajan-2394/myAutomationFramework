pipeline {
    agent any

    tools {
        maven 'MavenLocal'     // Must match Jenkins Maven name
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

        stage('Publish Reports') {
            steps {
                // Publish TestNG report
                testNG reportFilenamePattern: '**/test-output/testng-results.xml',
                      escapeTestDescp: true, escapeExceptionMsg: true

                // Publish JUnit results if available
                junit allowEmptyResults: true, testResults: '**/surefire-reports/*.xml'

                // Save reports & logs
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
