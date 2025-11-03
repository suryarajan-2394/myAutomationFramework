pipeline {
    agent any

    options {
        timestamps()
        ansiColor('xterm')
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

                // Publish JUnit XML if any
                junit allowEmptyResults: true, testResults: '**/surefire-reports/*.xml'

                // Save test-output & target artifacts (logs, screenshots, reports)
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
