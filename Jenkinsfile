pipeline {
    agent any

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
                testNG reportFilenamePattern: '**/test-output/testng-results.xml',
                      escapeTestDescp: true, escapeExceptionMsg: true

                junit allowEmptyResults: true, testResults: '**/surefire-reports/*.xml'

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
