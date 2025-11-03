pipeline {
  agent any

  tools {
    jdk 'jdk21'            
    maven 'Maven-3.9'    
  }

  stages {

    stage('Checkout Code') {
      steps {
        checkout scm
      }
    }

    stage('Build & Run Tests') {
      steps {
        bat 'mvn clean test -Dsurefire.suiteXmlFiles=testng.xml'
      }
    }

    stage('Publish TestNG Results') {
      steps {
        testNG testResultsPattern: '**/test-output/testng-results.xml'
      }
    }
  }

  post {
    success {
      echo '✅ Test Execution Successful'
    }
    failure {
      echo '❌ Test Execution Failed'
    }
  }
}
