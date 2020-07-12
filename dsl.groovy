
#!groovy
job("download_code_1") {
    description ("pull the code from github")
    scm {
    github ('hitebrata/devopstask6-dsl-job','master')
    }
    steps{
    shell(''' cp * -v /opt ''')
    }
}
job("configure_kubectl_2") {
    description ("download kubectl and configure on docker container")
    steps{
    shell('''
    mkdir -p /root/.kube
    curl -LO https://storage.googleapis.com/kubernetes-release/release/v1.18.0/bin/linux/amd64/kubectl
    chmod +x ./kubectl
    mv ./kubectl /usr/local/bin/kubectl
    kubectl version --client 
    cp -v /opt/config ~/.kube/
    cp -v /opt/client.key ~/.kube/
    cp -v /opt/client.crt ~/.kube/
    cp -v /opt/ca.crt ~/.kube/
    ''')
    }
    triggers {
        upstream('download_code_1', 'SUCCESS')
    }
}
job("deploy_code_3") {
    description ("find the code type and deploy on kubernetes environment")
    steps{
    shell('''
    if ls /opt | grep '[a-z].php'
    then
    echo "it's a php code"
        if kubectl get deploy | grep webserver
        then
        echo "php container is running"
        else
        kubectl create -f /opt/web-server-deploy.yml
        fi
    else
    echo "unable to find the php code"
    fi
    echo "copy the php code to kubernetes pods"
    pod_name=$(kubectl get pods | awk '{print $1}' | awk 'NR==2{print $1}')
    kubectl cp /opt/*.php   $pod_name:/var/www/html/
    ''' )
    }
    triggers {
        upstream('configure_kubectl_2', 'SUCCESS')
    }
}

job("testing_4") {
    description ("test the code is working or not")
    steps{
    shell(''' echo " testing web page"
    X=$(curl -o /dev/null -s -w "%{http_code}" "http://192.168.99.100:30100")
    if [ $X = "200" ]; then
    echo "web site is running"
    else
    curl http://192.168.60.123:9898/job/email_alert_5/build?token=send_mail_to_admin
    fi ''')
    
    triggers {
            upstream('deploy_code_3', 'SUCCESS')
    }
    
}
job('email_alert_5') {
    publishers {
        extendedEmail {
            recipientList('visnetbbsrzabbix@gmail.com')
            defaultSubject('Oops code failed....')
            defaultContent('Something broken')
            contentType('text/html')
            triggers {
                always {
                       sendTo {
                        recipientList('EMAILADDRESS@gmail.com')
                        }
                    
                }
            }
        }
    }
}