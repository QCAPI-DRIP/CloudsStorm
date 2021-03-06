/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * Copyright © Huan Zhou (SNE, University of Amsterdam) and contributors
 */
package lambdaInfrs.request;


import java.util.HashMap;
import java.util.Map;

import commonTool.ClassSet;

///This class is used to identify the vertical scaling for some VMs
///The scaled VM must be in the same datacenter
public class VScalingVMRequest {
	
	public class VMVScalingReqEle {
		
		/// identify this request
		public String reqID;
		
		public String orgVMName;
		
		///the target CPU/MEM which is going to be scaled vertically
		public double targetCPU;
		public double targetMEM;
		
		////this contains all the possible classes might be needed
		public ClassSet scaledClasses;
	}
	
	///Value means whether this request can be satisfied
	public Map<VMVScalingReqEle, Boolean> content = new HashMap<VMVScalingReqEle, Boolean>();
}
