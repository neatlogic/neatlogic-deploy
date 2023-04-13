[Chinese](README.md) / English

## about

Neatlogic-deploy is a publishing module that mainly includes functions such as application configuration, version center, and one click publishing.

## Feature

### Application Configuration

Application configuration mainly manages the configuration of pipeline scripts and parameters for application, module layer, and environment layer.
![img.png](README_IMAGES/img1.png)
![img.png](README_IMAGES/img.png)
- Support configuration application continuous integration.
- Support the configuration of application super pipelines, and only the current application can be selected in the template.
- Support for configuring notification policies.
- Support managing user scope through authorization.
- Support for editing application, module, and environmental information

### Version Center

The version center is used to manage the versions and engineering materials of application modules. The compiled version of the engineering physics library supports uploading and downloading.
![img.png](README_IMAGES/img2.png)
![img.png](README_IMAGES/img3.png)

### One-click publishing

One-click publishing page supports launching single publishing jobs and launching batch jobs.
1. Initiate a single publish job<br>
Select the application and module that initiated the job (with a configured assembly line and environment), and then click the Add Job button to complete the configuration of initiating and publishing the job and save it.
![img.png](README_IMAGES/img4.png)
![img.png](README_IMAGES/img5.png)
2. Batch Publish Job<br>
Batch publishing jobs can be created directly or initiated through a super assembly line. Direct creation refers to adding existing jobs to a collection, and the super pipeline method is initiated through templates.
![img.png](README_IMAGES/img6.png)
![img.png](README_IMAGES/img7.png)
![img.png](README_IMAGES/img8.png)

### Superpipeline

The Superpipeline manages global batch publishing job templates, and supports the initiation of batch jobs and scheduled jobs.
![img.png](README_IMAGES/img9.png)
![img.png](README_IMAGES/img10.png)

### Scheduled job
Scheduled job are initiated and published by configuring a fixed job timer, which supports two types of jobs: regular jobs and super pipeline jobs.
![img.png](README_IMAGES/img11.png)

 ### Webhook
 The webhook page is used to manage job triggers, including configuring trigger ranges and trigger actions. The trigger range is the trigger point, and the object of the trigger range is the job state associated with the application module's environment. The trigger action is an integrated configuration associated with the "Publish Trigger Data Specification" type.
 ![img.png](README_IMAGES/img12.png)