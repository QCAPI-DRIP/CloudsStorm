# DRIPProvisioningAgent
This is a **totally new** version of provisioner in DRIP subsystem of SWITCH project.
Comparing to the previous implementation, this implementation: 
- Use maven to construct the whole structure.
- Put all the previous three projects into together. 
- It will be easier to invoke. 
- Use log4j to do logging. 
- Add featues for user to stop (only can be done by EC2 currently) or terminate the subtopology.
- Add more features to support faliure recovery and auto scalling.  