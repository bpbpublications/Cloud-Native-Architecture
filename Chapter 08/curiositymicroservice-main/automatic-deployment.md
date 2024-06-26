**Introduction**

In the previous step, you have manually installed the application and test it with the browser and curl.  To replicate it in an automated way, by setting up a CI/CD pipeline, please follow the complete instructions here: https://github.com/fharris/curiositymicroservice/blob/main/automatic-deployment.md. You will certainly have a lot of fun doing it. In the end, you should be able to have an ecosystem containing a CI/CD toolchain powered by Jenkins and completely integrated with Gogs as a local Git server and Registry as a local Docker registry server. There is also a MySQL database running locally as a container. There is also a Dind or Docker in Docker container which is needed to help Jenkins container build containers. Everything will be running as a container inside a docker network called cloudnative network  For the runtime or the target against which Jenkins will be deploying, we are going to continue to use Kubernetes. **In our example, we are using Rancher Desktop with K3s. This is relevant because we already have a Traefik ingress controller installed.**  You can also see in the diagram the IP addresses for the cloudnative docker network (CIDR 172.18.0.0/16, which is predefined and shouldn’t change for you) and the Kubernetes cluster API (in our case 192.168.5.15:6443 and should almost certainly be different for you). Jenkins is the Master of Ceremony and will be responsible for managing builds and deployments. The exercises you are about to follow will test the efficiency of the CI/CD pipeline. The next Figure illustrates step-by-step, from 1 to 4, the flow of events that we need to learn to see how a change in the application's code will trigger the CI/CD toolchain and deploy a new version of the application. 

![image](https://github.com/fharris/curiositymicroservice/assets/17484224/d6706678-9870-4f19-9287-af25a1cb1f89)



**Requirements**

*16 GB RAM* (The Jenkins container in particular needs a bit more resources then the other. So, make sure that you have that available if you start noticing Jenkins slower than it should ;))

*Access to a Kubernetes cluster*

*kubectl* 

*git* 

*jq*

*MySQL client*

*docker*



**Get the code from GitHub**

If you haven't done so, get the code from the repository:
```
git clone https://github.com/fharris/curiositymicroservice
```
Change to the curiositymicroservice folder and run the following commands:

If you already tried this exercise before, or have installed the application manually, please run the following 2 scripts for housekeeping:

```
./housekeeping-k8s.sh
```

```
./housekeeping-docker.sh
```
1. **Take note of the kube proxy API endpoint.**

If you are running this exercise with a Kubernetes cluster whose API is a public IP or a private IP behind a public jump machine, then you just need to take note of that IP. If you are running this with a local Kubernetes cluster then you can just follow the next steps to get the IP of your local Kubernetes cluster.

In a tab run the command:

``
kubectl proxy &
``
Open another tab and run the following command:
``
curl localhost:8001/api
``
And you should get the Server address. 

<img width="339" alt="image" src="https://github.com/fharris/curiositymonolith/assets/17484224/de567643-96be-4683-901f-1fda0abf15af">

You will need to update this value later in Jenkins env vars.

2. **Create a namespace for the application and Jenkins user credentials**

```
./jenkinsconfiguration-k8.sh
```

3. **Create local network and containers**

We will create a docker network (we could do this with docker composer as well, but for now let's keep it like that) and provision 5 containers that will help us recreate a simplified cloud native ecosystem. A first container with Docker Dind which allows us to build and run containers from within containers. A second container with Jenkins where the CI/CD pipelines to build and deploy the application will be configured. A third container with Gogs, a simple git Server where our code will reside and be synchronized with the Jenkins pipelines. A fourth container with a local mysql database, which we will use to help us on the builds. A fifth container with a local docker registry where the lifecycle of our application image will be managed and Kubernetes pull it to launch. Remember that this exercise is pedagogical and has an educational goal. It's important if you want to learn how things work behind the scenes.

Run the script:
```
./CICD/containers-run-config.sh
```
At some point, you will see the screen below and the script will stop and wait for the Gogs server configuration. 

![image](https://github.com/fharris/curiositymonolith/assets/17484224/493c5a59-1abb-4f03-b8e7-288e94cded53)


Follow the next instructions to configure Gogs and when finished, return to the script in the terminal, and press Enter to resume the configuration of the other containers:

3. **GOGS: Configure local Git server**

The Gogs container is running on *http://localhost:10880*. Copy past that hostname:port on your browser and start the configuration. The first time you run it, you will get a special screen for the database set-up. Make sure that you select **SQLite3** and keep the **Path** as it is as illustrated in the next figure.

<img width="1036" alt="image" src="https://github.com/fharris/curiositymonolith/assets/17484224/fa65bd6b-305e-450d-9d92-4a3627c9146e">
                                

Below, in the Application General Settings please change the default Branch from **master** to **main** : as illustrated next:

<img width="942" alt="image" src="https://github.com/fharris/curiositymonolith/assets/17484224/9859ee68-2dfe-4e5d-ba2a-2287ea2c0c76">


Now on the optional settings, you will need to define an admin user called gogs-user. Take note of the password you are going to use. The email is optional. Click the blue button **Install Gogs** and follow figure gogs3 for more details:

<img width="959" alt="image" src="https://github.com/fharris/curiositymonolith/assets/17484224/33f8fcb5-91b6-4a28-ad09-fadd0e4d6b15">


![](RackMultipart20231003-1-aq9tt0_html_e1991ea04bc71612.png)

**Figure gogs3**

Now is the moment to return to the terminal and resume the script by pressing **ENTER**. 


![image](https://github.com/fharris/curiositymonolith/assets/17484224/bcc3d55e-70e6-447b-8f67-8cc137510fe7)


Let the script run to the end.
You will notice that this version of script has already created in the local database server both databases curiositydb and championshipdb for the 2 backend microservices.

![image](https://github.com/fharris/curiositymicroservice/assets/17484224/09c125e2-e1a4-493a-bd62-c89de9edc79d)


Lets confirm that all containers are running. You need **jq** installed to run the following command:

**[optional]**
```
docker network inspect cloudnative | jq '.[].Containers'
```

you should see the docker network cloudnative with 5 containers running, each showing their respective hostnames and local IP addresses:

![image](https://github.com/fharris/curiositymonolith/assets/17484224/166d4a97-b70a-439b-9853-739a49a21186)

**GOGS: Continue with Gogs Configuration**

Get back to the Gogs Console in the browser. Just retype  [http://localhost:10880](http://localhost:10880) and sign in with the user **gogs-user** and the password you created before:

![image](https://github.com/fharris/curiositymicroservice/assets/17484224/386f33d7-b9e5-47d8-b085-41ecd1f3f16d)



**GOGS: Migrate repositories from Github**

Click the little plus "+" signal next to your avatar and select *New Migration*:

<img width="985" alt="image" src="https://github.com/fharris/curiositymonolith/assets/17484224/303652e6-1048-4a79-ac4d-8b9600d64f72">


Replace the Clone Address with the GitHub address of the original repository which is in this case https://github.com/fharris/curiositymicroservice . The owner will be **gogs-user** and the name should be the **curiositymicroservice** as well.


![image](https://github.com/fharris/curiositymicroservice/assets/17484224/c55a92f8-9cad-4a0b-b61e-f0e94d640047)

Let's repeat the previous step to import the other 2 microservices code from GitHub, **championshipmicroservice** and **curiosityfrontendmicroservice**:

Click the little plus "+" signal next to your avatar. select New Migration and replace the Clone Address with the GitHub address of the original repository which is now https://github.com/fharris/championshipmicroservice and https://github.com/fharris/curiosityfrontendmicroservice respectively. The image below shows the example for the championshipmicroservice:

![image](https://github.com/fharris/curiositymicroservice/assets/17484224/7c3d36a7-0b35-47ad-9f8e-aeb8632743ed)

And the next image shows the example for the curiosityfrontendmicroservice:

![image](https://github.com/fharris/curiositymicroservice/assets/17484224/1aeb133f-8531-41cc-a506-dd0e6a202b61)


After clicking the green button to start the migration, if all goes well, you should be able to see your codebase of our 3 microservices at:
- [http://localhost:10880/gogs-user/curiositymicroservice](http://localhost:10880/gogs-user/curiositymicroservice)
- [http://localhost:10880/gogs-user/championshipmicroservice](http://localhost:10880/gogs-user/championshipmicroservice)
- [http://localhost:10880/gogs-user/curiosityfrontendmicroservice](http://localhost:10880/gogs-user/curiosityfrontendmicroservice)

As illustrated in the next figure:

![image](https://github.com/fharris/curiositymicroservice/assets/17484224/9e1ab4a4-8374-4771-808f-3328c10b45b8)
  


**GOGS: Create Webhooks for Jenkins**

We will now configure Webhooks for the Gogs-Jenkins communication. From your codebase click Settings (the little tools icon on the top right of the screen) or navigate directly to [http://localhost:10880/gogs-user/curiositymicroservice/settings](http://localhost:10880/gogs-user/curiositymicroservice/settings) . Click Webhooks, and Add a New Webooks of type Gogs as per next figure:

![image](https://github.com/fharris/curiositymicroservice/assets/17484224/6772103b-32d4-44f2-a05d-3baf4ccedf41)



Fill in the Payload URL with [http://jenkins:8080/gogs-webhook/?job=buildcuriosity](http://jenkins:8080/gogs-webhook/?job=buildcuriosity) which is where Jenkins CI/CD will be waiting for Gogs notifications for the curiosity microservice. Make sure the Content-Type is application/json, and that there is no Secret configured. Before clicking the green button to create the Webhook ensure the Just the Push event option is selected and the Active box is enabled. Next figure  shows how to do it:

<img width="997" alt="image" src="https://github.com/fharris/curiositymonolith/assets/17484224/07981dab-68ac-4420-971b-b5ecfe4eb257">


If all goes well, you should get something as the following figure. 

![image](https://github.com/fharris/curiositymicroservice/assets/17484224/c50f205b-232c-4cc6-9be4-f4c12abff5a2)

Click the name of the webhook, which should be the same as the Jenkins endpoint (...jenkins:8080/gogs-webhook/?job=buildcuriosity) and you should be back inside the webhook configuration:

![](RackMultipart20231003-1-aq9tt0_html_13b8dd7759df0b57.png)

Figure gogs8

Now, inside the webhook configuration you will see a little **Test Delivery** button on the right bottom of the screen: 

![image](https://github.com/fharris/curiositymicroservice/assets/17484224/073c1ba7-99fa-4a24-8486-7d5708b39e22)


If you click it you should get a successful test and an event delivered to your local Jenkins:

<img width="1090" alt="image" src="https://github.com/fharris/curiositymonolith/assets/17484224/fb89e807-40c2-47da-b1a9-f53d3839553a">


![](RackMultipart20231003-1-aq9tt0_html_6f9154031c81211.png)

Figure gogs9

Repeat the previous steps to create the webhooks for the other 2 services:

Navigate directly to [http://localhost:10880/gogs-user/championshipmicroservice/settings](http://localhost:10880/gogs-user/championshipmicroservice/settings) .  Click Webhooks, and Add a New Webooks of type Gogs. Fill in the Payload URL with [http://jenkins:8080/gogs-webhook/?job=buildchampionship](http://jenkins:8080/gogs-webhook/?job=buildchampionship) . Click the link for the webhook, which should be the same as the Jenkins endpoint (...jenkins:8080/gogs-webhook/?job=buildchampionship) and press the **Test Delivery** button on the right bottom of the screen. The following image illustrates the process for the championshipmicroservice:

![image](https://github.com/fharris/curiositymicroservice/assets/17484224/f9263db3-5e07-497d-b5e6-0fd9628c6585)


Repeat the process for curiosityfrontendmicroservice. Navigate directly to [http://localhost:10880/gogs-user/curiosityfrontendmicroservice/settings](http://localhost:10880/gogs-user/curiosityfrontendmicroservice/settings) .  Click Webhooks, and Add a New Webooks of type Gogs. Fill in the Payload URL with [http://jenkins:8080/gogs-webhook/?job=buildcuriosityfrontend](http://jenkins:8080/gogs-webhook/?job=buildcuriosityfrontend) . Click the link for the webhook, which should be the same as the Jenkins endpoint (...jenkins:8080/gogs-webhook/?job=buildcuriosityfrontend). Press the **Test Delivery** button on the right bottom of the screen:

![image](https://github.com/fharris/curiositymicroservice/assets/17484224/38177031-1e34-426a-8a61-eb4eed4bcb74)



4. **Configuring Jenkins**

This Jenkins container has all the CI/CD pipelines already configured for you to use. All is managed as code from the code base repository. Navigate with your browser to localhost:8080 and sign in with the user we prepared for you which is **admin** with password **123**. Skip all the steps related to plugin installation or related to the creation of new users.

<img width="1281" alt="image" src="https://github.com/fharris/curiositymonolith/assets/17484224/9aea823c-2a7a-422a-ad10-70c7df68aab8">

![](RackMultipart20231003-1-aq9tt0_html_647a325c2ba2b80e.png)

Close everything and ignore

<img width="1013" alt="image" src="https://github.com/fharris/curiositymonolith/assets/17484224/bf148835-c175-4c7c-b6b6-2b96fa0d18c5">


![](RackMultipart20231003-1-aq9tt0_html_7ccf605345a563ea.png)

Start using Jenkins
<img width="1001" alt="image" src="https://github.com/fharris/curiositymonolith/assets/17484224/82f9ba77-4d72-4d56-a7c7-8b44cb6c196f">


Once logged in you should see Jenkins with 9 jobs configured. Three for each microservice. The 3 build jobs (buildcuriosity, buildchampionship and buildcuriosityfrontend) should have at least one failed build, which was triggered when you tested the Gogs Webhook. The first run takes a few minutes:


![image](https://github.com/fharris/curiositymicroservice/assets/17484224/56978b4a-65a4-4110-a5e2-7e292b518e19)



We must update a couple of things first. The Kubernetes token for the Jenkins Service Account, the local MySQL password to the **curiosity** user, which is **Welcome#1** and the Kubernetes endpoint.


In *Manage Jenkins*,  Click *Credentials*:
<img width="1078" alt="image" src="https://github.com/fharris/curiositymonolith/assets/17484224/5d6fb621-7165-499b-ae71-88522f81ef82">


**Update Kubernetes token:**


Select the **jenkins-token-kubernetes** to edit and replace with the token you generated before.

<img width="1387" alt="image" src="https://github.com/fharris/curiositymonolith/assets/17484224/c5341eae-8ef5-41ec-b8ae-6fb1fa256f90">


If you don't remember the token, open a terminal and run the following command to get it again:

```
kubectl get secrets jenkins-task-sa-secret -o json | jq -Mr '.data["token"]' | base64 -D
```

and after clicking *Update*, replace the secret with it:
<img width="1346" alt="image" src="https://github.com/fharris/curiositymonolith/assets/17484224/8e5febeb-2bad-4e0d-bb14-0cfdc52e55b9">


![](RackMultipart20231003-1-aq9tt0_html_4b65a4753b9b0fad.png)

![](RackMultipart20231003-1-aq9tt0_html_e47bcee192f08e62.png)

Click Save.

**Update Local MySQL password:**

Repeat the exercise for the MySQL database.

Select the id-mysql credential to update:
<img width="1382" alt="image" src="https://github.com/fharris/curiositymonolith/assets/17484224/a6d0d104-52b5-4db4-b0b3-aa96c629d9af">

Keep the username curiosity and replace the password with Welcome#1    :
<img width="1371" alt="image" src="https://github.com/fharris/curiositymonolith/assets/17484224/2ee875ae-b34e-4730-98cc-467acc61119a">

Click *Save*.

**Update Kubernetes API Proxy endpoint**

In Manage Jenkins, click System or go directly to http://localhost:8080/manage/configure :

![Alt text](image.png)

Search the environment variables and update with the server address of your Kubernetes cluster:
<img width="1390" alt="image" src="https://github.com/fharris/curiositymonolith/assets/17484224/c6f839b4-822c-4e25-8117-29cd8af695d0">

Click *Save*.


Back to the Dashboard schedule a build for the job **buildcuriosity** as we need to generate an image to pull to the local container repos:

![image](https://github.com/fharris/curiositymicroservice/assets/17484224/29666b3b-2861-483c-b3fc-79414c861908)



When the job is finished, if the Building Image step is green 


![image](https://github.com/fharris/curiositymicroservice/assets/17484224/7845111f-6038-47af-9d54-71e7a46e7f1e)


and its logs show that the image was pushed to the local repository, then we should be OK to continue:

![image](https://github.com/fharris/curiositymicroservice/assets/17484224/aba2aef6-62cc-43dc-b15e-91eb9e6f33c2)


![](RackMultipart20231003-1-aq9tt0_html_be3cc375e5563d84.png)

Return to the Dashboard and run the **configurecuriosity** job to install the application and database in Kubernetes:

![image](https://github.com/fharris/curiositymicroservice/assets/17484224/571287e6-1484-4e8b-a239-c0fa0f407403)

This version of the **configurecuriosity** installs Kafka and Zookeeper besides the MySQL database and runs only once, when things are being set up for the first time.

![image](https://github.com/fharris/curiositymicroservice/assets/17484224/bcccc2d5-d79e-41f5-82c5-a1977d2fa3fd)


![](RackMultipart20231003-1-aq9tt0_html_9099957d1166cc4d.png)

![](RackMultipart20231003-1-aq9tt0_html_623f9f7e813e664e.png)

If the job fails, give it a new try because there is a command that takes a bit longer to run the first time it runs.

When the 3 jobs for the curiosity microservice are green, then the CI/CD pipeline is set for it. Now, whenever you need to make a change in the code, all you need to do is push the new version of the code and the build and deploy jobs will run and take care of the rest. To confirm the first deployment is OK, run the following command in your terminal:
```
kubectl -n curiosityevents get pods
```

and you should see the pods running in the namespace:

![image](https://github.com/fharris/curiositymicroservice/assets/17484224/3e70295d-7cef-40dc-8382-e1903902de90)


To deploy the **championshipmicroservice** you just need to repeat these last steps:

- Schedule a build for the buildchampionship job: 

  ![image](https://github.com/fharris/curiositymicroservice/assets/17484224/51c81e2a-4468-4b97-be63-87cf0ddb3862)

  this will build and push the image to the local repository:

  ![image](https://github.com/fharris/curiositymicroservice/assets/17484224/519a7d36-208d-442e-9d6e-e7727b62f1c0)


- Schedule a build for the configurechampionship job:

  ![image](https://github.com/fharris/curiositymicroservice/assets/17484224/b7c361a0-e927-4d98-9146-249ee45a4959)


this will configure things to be used by the microservice, such as the database, etc: 

![image](https://github.com/fharris/curiositymicroservice/assets/17484224/5145d3c6-7162-434c-a0d5-cc9fb31cb9e2)


- Schedule a new build for the buildchampionship job:

  ![image](https://github.com/fharris/curiositymicroservice/assets/17484224/6c394eef-9d6d-463e-8e35-3d4de214e69c)


this will rebuild the image, push it and automatically trigger the deploychampionship job as well.

![image](https://github.com/fharris/curiositymicroservice/assets/17484224/4f72f39e-6b03-4386-aa4b-6ea68d9afc37)



When the 3 jobs for the championship microservice are green, then the CI/CD pipeline is set for it. 

![image](https://github.com/fharris/curiositymicroservice/assets/17484224/693e1793-9923-4016-8e3b-d534ba6caf04)


Now, whenever you need to make a change in the code, all you need to do is push the new version of the code and the build and deploy jobs will run and take care of the rest. To confirm the first deployment is OK, run the following command in your terminal:
```
kubectl -n curiosityevents get pods
```

and you should see the championship pods, called consumerms running in the namespace as well:

![image](https://github.com/fharris/curiositymicroservice/assets/17484224/82c9f340-db95-4a06-939e-7fb0c2a2291e)



To deploy the **curiosityfrontendmicroservice** you just need to repeat the exact same  steps:

- Schedule a build for the buildcuriosityfrontend job;
 
![image](https://github.com/fharris/curiositymicroservice/assets/17484224/62b2419e-2e58-4c17-bcec-17ff5af5fb42)

- Schedule a build for the configurecuriosityfrontend job which will install the ingress configuration for Traefik:

![image](https://github.com/fharris/curiositymicroservice/assets/17484224/4423a794-866d-484a-8962-0fd436259ba5)



- Schedule a new build for the buildcuriosityfrontend job;

  ![image](https://github.com/fharris/curiositymicroservice/assets/17484224/45d47074-ff6c-4d9b-afb9-5eced353bc13)


When the 3 jobs for the curiosityfrontend microservice are green, then the CI/CD pipeline is set for it. 

![image](https://github.com/fharris/curiositymicroservice/assets/17484224/fa03ce42-667a-4d4e-aa45-b44f3715297e)

Now, whenever you need to make a change in the code, all you need to do is push the new version of the code and the build and deploy jobs will run and take care of the rest. To confirm the first deployment is OK, run the following command in your terminal:
```
kubectl -n curiosityevents get pods
```

and you should see the curiosity frontend pods running in the namespace as well:

![image](https://github.com/fharris/curiositymicroservice/assets/17484224/479f2d7d-eea9-4110-adcc-a670db1e0be4)


As we are relying on a Traefik ingress controller for our local K3s Kubernetes cluster, you should see the application running in your browser at **HTTP://localhost:3000** :

![image](https://github.com/fharris/curiositymicroservice/assets/17484224/193fbae0-1578-44d1-9fb7-e27e4277a006)

Try the application against its APIs as well with curl. From another terminal tab run the following examples:
```
curl localhost/wiki/curiosity/users/1
```
```
curl localhost/wiki/curiosity/topics/Fernando/Spain
```
```
curl localhost/wiki/curiosity/page/Fernando/Portugal/Spain
```
```
curl localhost/leaders/
```



And that's it!!

The application is running and the CICD with Jenkins is ready! 
Try to clone the code and make some changes to see the CI/CD being triggered now!

Have fun!
