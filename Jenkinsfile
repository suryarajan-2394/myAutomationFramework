pipeline {
  agent any

  tools {
    maven 'MavenLocal'   // adjust if you named Maven differently in Jenkins global tool config
  }

  options { timestamps() }

  stages {
    stage('Checkout Code') {
      steps { checkout scm }
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
      // do not fail pipeline permanently for testing - we'll mark unstable if tests fail
    }

    stage('Publish Extent Report (HTML)') {
      steps {
        publishHTML([
          reportDir: 'AutomationReports',
          reportFiles: 'TestAutomationReport.html',
          reportName: 'Extent Report',
          keepAll: true,
          alwaysLinkToLastBuild: true,
          allowMissing: true
        ])
      }
    }

    stage('Zip AutomationReports') {
      steps {
        script {
          // delete old zip if exists then create new one
          if (isUnix()) {
            sh '''
              rm -f AutomationReports.zip || true
              zip -r AutomationReports.zip AutomationReports || true
            '''
          } else {
            bat '''
              if exist AutomationReports.zip del /F /Q AutomationReports.zip
              powershell -NoProfile -Command "Compress-Archive -Path 'AutomationReports\\*' -DestinationPath 'AutomationReports.zip' -Force"
            '''
          }
        }
      }
    }

    stage('Archive Artifacts') {
      steps {
        archiveArtifacts artifacts: 'AutomationReports.zip, AutomationReports/**, target/**, test-output/**', fingerprint: true
      }
    }
  }

  post {
    always {
      script {
        // send email with attached zip (replace credential id if you used different id)
        // emailext uses Global Extended Email Notification unless you pass different smtpServer etc.
        def subject = "${env.JOB_NAME} #${env.BUILD_NUMBER} - ${currentBuild.currentResult}"
        def body = """
          <p>Build: <a href='${env.BUILD_URL}'>${env.BUILD_URL}</a></p>
          <p>Result: ${currentBuild.currentResult}</p>
          <p>See attached AutomationReports.zip</p>
        """
        // Ensure credential id matches the one you added to Jenkins (username + app password)
        emailext(
          to: 'suryarajan.selvarajan@gmail.com',
          subject: subject,
          body: body,
          mimeType: 'text/html',
          attachPatterns: 'AutomationReports.zip',
          from: 'suryarajan.selvarajan@gmail.com',
          replyTo: 'suryarajan.selvarajan@gmail.com',
          // IMPORTANT: this must match the credential ID you created
          credentialsId: 'gmail-creds'
        )
      }
    }

    success { echo "✅ Build passed" }
    unstable { echo "⚠️ Build unstable (tests failed)" }
    failure { echo "❌ Build failed" }
  }
}
