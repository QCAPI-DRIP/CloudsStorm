package provisioning.engine.VEngine.EC2;

import java.util.ArrayList;
import java.util.List;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.AttachInternetGatewayRequest;
import com.amazonaws.services.ec2.model.AttachVolumeRequest;
import com.amazonaws.services.ec2.model.AuthorizeSecurityGroupIngressRequest;
import com.amazonaws.services.ec2.model.CreateInternetGatewayRequest;
import com.amazonaws.services.ec2.model.CreateInternetGatewayResult;
import com.amazonaws.services.ec2.model.CreateKeyPairRequest;
import com.amazonaws.services.ec2.model.CreateKeyPairResult;
import com.amazonaws.services.ec2.model.CreateRouteRequest;
import com.amazonaws.services.ec2.model.CreateSecurityGroupRequest;
import com.amazonaws.services.ec2.model.CreateSecurityGroupResult;
import com.amazonaws.services.ec2.model.CreateSubnetRequest;
import com.amazonaws.services.ec2.model.CreateSubnetResult;
import com.amazonaws.services.ec2.model.CreateVolumeRequest;
import com.amazonaws.services.ec2.model.CreateVolumeResult;
import com.amazonaws.services.ec2.model.CreateVpcRequest;
import com.amazonaws.services.ec2.model.CreateVpcResult;
import com.amazonaws.services.ec2.model.DeleteInternetGatewayRequest;
import com.amazonaws.services.ec2.model.DeleteSecurityGroupRequest;
import com.amazonaws.services.ec2.model.DeleteSubnetRequest;
import com.amazonaws.services.ec2.model.DeleteVpcRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.DescribeRegionsResult;
import com.amazonaws.services.ec2.model.DescribeRouteTablesRequest;
import com.amazonaws.services.ec2.model.DescribeRouteTablesResult;
import com.amazonaws.services.ec2.model.DescribeSubnetsRequest;
import com.amazonaws.services.ec2.model.DescribeSubnetsResult;
import com.amazonaws.services.ec2.model.DetachInternetGatewayRequest;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.InstanceStateName;
import com.amazonaws.services.ec2.model.IpPermission;
import com.amazonaws.services.ec2.model.KeyPair;
import com.amazonaws.services.ec2.model.ModifySubnetAttributeRequest;
import com.amazonaws.services.ec2.model.ModifyVpcAttributeRequest;
import com.amazonaws.services.ec2.model.Region;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.ec2.model.RouteTable;
import com.amazonaws.services.ec2.model.RunInstancesRequest;
import com.amazonaws.services.ec2.model.RunInstancesResult;
import com.amazonaws.services.ec2.model.TerminateInstancesRequest;
import com.amazonaws.services.ec2.model.VolumeType;

public class EC2Agent {
	
	
	private AmazonEC2Client ec2Client;
	
	public EC2Agent(String accessKey, String secretKey){
		BasicAWSCredentials credentials = 
				new BasicAWSCredentials(accessKey, secretKey);
		ec2Client = new AmazonEC2Client(credentials);
	}
	
	
	public String createKeyPair(String keyName){
		CreateKeyPairRequest createKeyPairRequest = new CreateKeyPairRequest();
		createKeyPairRequest.withKeyName(keyName);
		CreateKeyPairResult createKeyPairResult =
				  ec2Client.createKeyPair(createKeyPairRequest);
		KeyPair keyPair = new KeyPair();
		keyPair = createKeyPairResult.getKeyPair();
		String privateKey = keyPair.getKeyMaterial();
		return privateKey;
	}
	
	public void setEndpoint(String endpoint){
		ec2Client.setEndpoint(endpoint);
	}
	
	public String createVPC(String vpcCIDR){
		CreateVpcRequest request = new CreateVpcRequest().withCidrBlock(vpcCIDR);
	    CreateVpcResult result = ec2Client.createVpc(request);
	    String vpcId = result.getVpc().getVpcId();
	    return vpcId;
	}
	
	/**
	 * Create a disk from the EC2 with a specified disk size and volume type according to the IOPS required.
	 * This can be attached to a instance latter. The subnetId must be one of the input,
	 * because the volume must be created at the same availability zone of the instance.
	 * About the EC2 volume type, can be found below. 
	 * @see <a href="ec2">https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/EBSVolumeTypes.html?icmpid=docs_ec2_console</a>
	 * 
	 * @return volumeId
	 */
	public String createVolume(int totalDiskSize, int IOPS, String subnetId){
		CreateVolumeRequest createVolumeRequest = new CreateVolumeRequest();
		int diskSize = totalDiskSize - 8;  ///This is the disk size for the attached volume
		if(diskSize <= 0){
			return null;
		}
		if(diskSize < 4){
			createVolumeRequest.setVolumeType(VolumeType.Gp2);
		}else{
			if(IOPS == 0)
				createVolumeRequest.setVolumeType(VolumeType.Gp2);
			else{
				createVolumeRequest.setVolumeType(VolumeType.Io1);
				createVolumeRequest.setIops(IOPS);
			}
		}
		
		ArrayList<String> subnetIds = new ArrayList<String>();
		subnetIds.add(subnetId);
		DescribeSubnetsRequest describeSubnetsRequest = new DescribeSubnetsRequest();
		describeSubnetsRequest.setSubnetIds(subnetIds);
		DescribeSubnetsResult describeSubnetResult = ec2Client.describeSubnets(describeSubnetsRequest);
		String availabilityZone = describeSubnetResult.getSubnets().get(0).getAvailabilityZone();
		
		createVolumeRequest.setSize(diskSize);
		
		//DescribeAvailabilityZonesResult daz = ec2Client.describeAvailabilityZones();
		createVolumeRequest.setAvailabilityZone(availabilityZone);
		CreateVolumeResult createVolumeResult = ec2Client.createVolume(createVolumeRequest);
		return createVolumeResult.getVolume().getVolumeId();
		
	}
	
	public void attachVolume(String volumeId, String instanceId){
		AttachVolumeRequest attachVolumeRequest = new AttachVolumeRequest();
		attachVolumeRequest.setVolumeId(volumeId);
		attachVolumeRequest.setInstanceId(instanceId);
		attachVolumeRequest.setDevice("/dev/sdh");
		ec2Client.attachVolume(attachVolumeRequest);
		
	}
	
	public String getAssociateRouteTableId(String vpcId){
		System.out.println(vpcId);
		DescribeRouteTablesRequest describeRouteTablesRequest = new DescribeRouteTablesRequest();
	    DescribeRouteTablesResult describeRouteTablesResult = ec2Client.describeRouteTables(describeRouteTablesRequest);
	    List<RouteTable> rTables = describeRouteTablesResult.getRouteTables();
	    String routeTableId = "";
	    for(int i = 0 ; i<rTables.size() ; i++){
	    	String tmpVpcId = rTables.get(i).getVpcId();
	    	System.out.println(tmpVpcId+" "+rTables.get(i).getRouteTableId());
	    	if(tmpVpcId.equals(vpcId)){
	    		routeTableId = rTables.get(i).getRouteTableId();
	    		break;
	    	}
	    }
		return routeTableId;
	}
	
	public String createInternetGateway(String vpcId){
		CreateInternetGatewayRequest createInternetGatewayReq = new CreateInternetGatewayRequest();
	    CreateInternetGatewayResult createInternetGatewayResult = ec2Client.createInternetGateway(createInternetGatewayReq);
	    String internetGatewayId = createInternetGatewayResult.getInternetGateway().getInternetGatewayId();
	    AttachInternetGatewayRequest attachInternetGatewayRequest = new AttachInternetGatewayRequest()
	    		.withInternetGatewayId(internetGatewayId).withVpcId(vpcId);
	    ec2Client.attachInternetGateway(attachInternetGatewayRequest);
	    return internetGatewayId;
	}
	
	public void createRouteToGate(String routeTableId, String internetGatewayId, String destionationCIDR){
		CreateRouteRequest createRouteRequest = new CreateRouteRequest()
	    		.withRouteTableId(routeTableId).withGatewayId(internetGatewayId)
	    		.withDestinationCidrBlock("0.0.0.0/0");
	    ec2Client.createRoute(createRouteRequest);
	}
	
	public void enableVpcDNSHostName(String vpcId){
		ModifyVpcAttributeRequest modifyVpcAttributeReq = new ModifyVpcAttributeRequest()
	    		.withVpcId(vpcId)
	    		.withEnableDnsHostnames(true);
	    ec2Client.modifyVpcAttribute(modifyVpcAttributeReq);
	}
	
	public String createSubnet(String vpcId, String CIDR){
		CreateSubnetRequest subnetReq = new CreateSubnetRequest().withVpcId(vpcId).withCidrBlock(CIDR);
	    CreateSubnetResult subnetResult = ec2Client.createSubnet(subnetReq);
	    String subnetId = subnetResult.getSubnet().getSubnetId();
		return subnetId;
	}
	
	public void enableMapPubAddress(String subnetId){
		ModifySubnetAttributeRequest modifySubnetAttributeReq = new ModifySubnetAttributeRequest()
	    		.withSubnetId(subnetId).withMapPublicIpOnLaunch(true);
	    ec2Client.modifySubnetAttribute(modifySubnetAttributeReq);
	}
	
	public String createBasicSecurityGroup(String vpcId, String groupName, String description){
		CreateSecurityGroupRequest csgr = new CreateSecurityGroupRequest().withVpcId(vpcId);
		csgr.withGroupName(groupName).withDescription(description);
		CreateSecurityGroupResult createSecurityGroupResult = ec2Client.createSecurityGroup(csgr);
		String securityGroupId = createSecurityGroupResult.getGroupId();
		IpPermission ipPermission = new IpPermission();
		ipPermission.withIpRanges("0.0.0.0/0")
						.withIpProtocol("-1")
			            .withFromPort(0)
			            .withToPort(65535);
		AuthorizeSecurityGroupIngressRequest authorizeSecurityGroupIngressRequest =
			    new AuthorizeSecurityGroupIngressRequest();
		authorizeSecurityGroupIngressRequest.withGroupId(securityGroupId)
			                                    .withIpPermissions(ipPermission);
		ec2Client.authorizeSecurityGroupIngress(authorizeSecurityGroupIngressRequest);
		return securityGroupId;
	}
	
	public String runInstance(String subnetId, String securityGroupId, 
			String imageId, String privateIpAddress, String instanceType, String keyName){
		RunInstancesRequest runInstancesRequest =
			      new RunInstancesRequest();

		runInstancesRequest.withImageId(imageId)
		.withSubnetId(subnetId).withSecurityGroupIds(securityGroupId).withPrivateIpAddress(privateIpAddress)
			                     .withInstanceType(instanceType)
			                     .withMinCount(1)
			                     .withMaxCount(1)
			                     .withKeyName(keyName)
			                     ;
		RunInstancesResult runInstancesResult =
			      ec2Client.runInstances(runInstancesRequest);
		Reservation rv = runInstancesResult.getReservation();
		List <Instance> ins = rv.getInstances();
		String instanceId = ins.get(0).getInstanceId();
		return instanceId;
	}
	
	
	////Get the public address of the spcified instanceId.
	////Wait for 100s for maximum.
	public String getPublicAddress(String instanceId){
		ArrayList<String> instanceIds = new ArrayList<String>();
		instanceIds.add(instanceId);
		DescribeInstancesRequest describeInstancesRequest =  new DescribeInstancesRequest()
				.withInstanceIds(instanceIds);
		int count = 0;
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
		
		while(true){
			DescribeInstancesResult describeInstancesResult = ec2Client.describeInstances(describeInstancesRequest);
		    List<Reservation> reservations = describeInstancesResult.getReservations();
	
		    for(int i = 0 ; i<reservations.size() ; i++){
			    	List<Instance> instances = reservations.get(i).getInstances();
			    	for(int j = 0 ; j<instances.size() ; j++)
			    	{
			    		if(instances.get(j).getInstanceId().equals(instanceId)){
			    			if(instances.get(j).getPublicIpAddress() == null)
			    				continue;
			    			else
			    				return instances.get(j).getPublicIpAddress();
			    		}
			    	}
		    }
	        try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
				return null;
			}
	        count++;
	        if(count > 1000)
	        		break;
		}
		return null;
	}
	
	
	public void terminateInstances(ArrayList<String> instances){
		TerminateInstancesRequest tir = new TerminateInstancesRequest().withInstanceIds(instances);
		ec2Client.terminateInstances(tir);
		while(true){
			DescribeInstancesRequest dis = new DescribeInstancesRequest().withInstanceIds(instances);
			DescribeInstancesResult disr = ec2Client.describeInstances(dis);
			List<Reservation> reservations = disr.getReservations();
			boolean allTerminated = true;
			int instanceCount = 0;
			for(Reservation reservation : reservations){
				for(Instance instance : reservation.getInstances()){
					instanceCount++;
					if(!instance.getState().getName().equals(InstanceStateName.Terminated.toString()))
						allTerminated = false;
				}
			}
			if(allTerminated && instanceCount == instances.size())
				break;
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	public void deleteVpc(String vpcId, String subnetId, 
			String securityGroupId, String internetGatewayId){
		DeleteSubnetRequest dsreq = new DeleteSubnetRequest().withSubnetId(subnetId);
		ec2Client.deleteSubnet(dsreq);
		DetachInternetGatewayRequest digreq = new DetachInternetGatewayRequest()
				.withInternetGatewayId(internetGatewayId)
				.withVpcId(vpcId);
		ec2Client.detachInternetGateway(digreq);
		DeleteInternetGatewayRequest deleteInternetGatewayRequest = new DeleteInternetGatewayRequest().withInternetGatewayId(internetGatewayId);
		ec2Client.deleteInternetGateway(deleteInternetGatewayRequest);
		DeleteSecurityGroupRequest dsgreq = new DeleteSecurityGroupRequest().withGroupId(securityGroupId);
		ec2Client.deleteSecurityGroup(dsgreq);
		DeleteVpcRequest dvreq = new DeleteVpcRequest().withVpcId(vpcId);
		ec2Client.deleteVpc(dvreq);
	}

	public String getAvailableRegions(String format){
		
		DescribeRegionsResult region = ec2Client.describeRegions();
		List<Region> regions = region.getRegions();
		String result = "";
		if(format.equals("plain")){
			for(int i = 0 ; i<regions.size() ; i++)
				result += regions.get(i).toString().substring(1, regions.get(i).toString().length()-1)+"\n";
		}
		
		return result;
	}


}