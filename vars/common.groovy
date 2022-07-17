def pipelineInit(){
    stage('Intiate Repo'){
        sh 'rm -rf *'
        git branch: 'main', url: "https://github.com/krishnavamsi7616/${COMPONENT}.git"
    }
}


def publishArtifacts() {
    env.ENV = "dev"
    stage("Prepare Artifacts") {
        if (env.APP_TYPE == "nodejs"){
        sh """
            zip -r ${ENV}-${COMPONENT}-${TAG_NAME}.zip node_modules server.js
        """
        }
        if (env.APP_TYPE == "maven"){
            sh """
            cp target/${COMPONENT}-1.0.jar ${COMPONENT}.jar
            zip -r ${ENV}-${COMPONENT}-${TAG_NAME}.zip ${COMPONENT}.jar
        """
        }
        if (env.APP_TYPE == "python"){
            sh """
            zip -r ${ENV}-${COMPONENT}-${TAG_NAME}.zip *.py ${COMPONENT}.ini requirements.txt
        """
        }
        if (env.APP_TYPE == "nginx"){
            sh """
            cd static
            zip -r ../${ENV}-${COMPONENT}-${TAG_NAME}.zip *
        """
        }
    }
    stage('Push Artifacts to Nexus'){
        withCredentials([usernamePassword(credentialsId: 'NEXUS', passwordVariable: 'pass', usernameVariable: 'user')]) {
            sh """
               curl -v -u ${user}:${pass} --upload-file ${ENV}-${COMPONENT}-${TAG_NAME}.zip http://172.31.1.200:8081/repository/${COMPONENT}/${ENV}-${COMPONENT}-${TAG_NAME}.zip
            """

        }

    }

    stage('Deploy to Dev Env'){
        dir('ANSIBLE') {
            git branch: 'main', url: "https://github.com/krishnavamsi7616/roboshop-ansible.git"
            sh """
                ansible-playbook -i ${COMPONENT}-${}
             """
        }
    }

}


def codeChecks(){
    stage('Quality Checks & Unit Tests') {
        parallel([
                qualityChecks: {
                    withCredentials([usernamePassword(credentialsId: 'SONAR', passwordVariable: 'pass', usernameVariable: 'user')]){
                    // sh "sonar-scanner -Dsonar.projectKey=${COMPONENT} -Dsonar.host.url=http://172.31.0.43:9000 -Dsonar.login=${user} -Dsonar.password=${pass} ${EXTRA_OPTS}"
                    // sh "sonar-quality-gate.sh ${user} ${pass} 172.31.0.43 ${COMPONENT}"
                        echo "Code Analysis"
                        }
                },
                unitTests: {
                    unitTests()
                }
        ])
    }
}

def unitTests(){

    if (env.APP_TYPE == "nodejs"){
        sh """
           # npm run test
           echo Run test cases
        """
    }
    if (env.APP_TYPE == "maven"){
        sh """
        # mvn test
        echo Run test cases
       
            """
    }
    if (env.APP_TYPE == "python"){
        sh """
            # python -m unittest
             echo Run Test Cases
             """
    }
    if (env.APP_TYPE == "nginx"){
        sh """
            # npm run test
            echo Run Test Cases
           """
    }
}
