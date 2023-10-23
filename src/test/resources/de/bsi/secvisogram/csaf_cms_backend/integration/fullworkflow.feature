Feature: Full workflow

  Background:
      * url 'http://localhost:8081'
      * def apiBase = '/api/v1/advisories' 

  Scenario: oauth 2 flow
    # Login
	Given url 'http://localhost:9000/realms/csaf/protocol/openid-connect/token'
	* form field client_id = 'secvisogram'
	* form field username = 'all'
	* form field password = 'all'
	* form field grant_type = 'password'
	* form field response_type = 'code'
	* form field audience = 'secvisogram'
	* form field requested_token_type = 'ID'
	When method post
	Then status 200
	* def accessToken = response.access_token
	* def refreshToken = response.refresh_token
	* def session = response.session_state

	########
	#Upload
	Given url 'http://localhost:8081'
	* path apiBase
	* header Authorization = 'Bearer ' + accessToken
	* header Content-Type = 'application/json'
	* request read('min.json')
	When method post
	Then status 201
	* def advisoryId = response.id
	* def revision = response.revision

	########	
	#Change workflow state to Review
	#
	# Draft, Review, Approved, RfPublication, Published
	* def workflowStatus = 'Review'
	Given path apiBase + '/' + advisoryId + '/workflowstate/' + workflowStatus
	* param revision = revision
	* header Authorization = 'Bearer ' + accessToken
	* header Content-Type = 'application/json'
	When method patch
	Then status 200
	
	# Get new revision after workflow state change
	Given path apiBase
	* header Authorization = 'Bearer ' + accessToken
	* header Content-Type = 'application/json'
	When method get
	Then status 200
	* def filt = function(x){ return x.advisoryId == advisoryId }
    * def items = get response[*]
    * def revision = karate.filter(items, filt)[0].revision    

	########	
	#Change workflow state to Approved
	#
	* def workflowStatus = 'Approved'
	Given path apiBase + '/' + advisoryId + '/workflowstate/' + workflowStatus
	* param revision = revision
	* header Authorization = 'Bearer ' + accessToken
	* header Content-Type = 'application/json'
	When method patch
	Then status 200
	# Get new revision after workflow state change
	Given path apiBase
	* header Authorization = 'Bearer ' + accessToken
	* header Content-Type = 'application/json'
	When method get
	Then status 200
	* def filt = function(x){ return x.advisoryId == advisoryId }
    * def items = get response[*]
    * def revision = karate.filter(items, filt)[0].revision    

	########	
	#Change workflow state to RfPublication
	#
	* def workflowStatus = 'RfPublication'
	Given path apiBase + '/' + advisoryId + '/workflowstate/' + workflowStatus
	* param revision = revision
	* header Authorization = 'Bearer ' + accessToken
	* header Content-Type = 'application/json'
	When method patch
	Then status 200
	# Get new revision after workflow state change
	Given path apiBase
	* header Authorization = 'Bearer ' + accessToken
	* header Content-Type = 'application/json'
	When method get
	Then status 200
	* def filt = function(x){ return x.advisoryId == advisoryId }
    * def items = get response[*]
    * def revision = karate.filter(items, filt)[0].revision 

	########	
	#Change workflow state to Published
	#
	* def workflowStatus = 'Published'
	Given path apiBase + '/' + advisoryId + '/workflowstate/' + workflowStatus
	* param revision = revision
	# final, interim, draft
	* param documentTrackingStatus = 'Final'
	* header Authorization = 'Bearer ' + accessToken
	* header Content-Type = 'application/json'
	When method patch
	Then status 200
		
	# Get new revision after workflow state change
	Given path apiBase
	* header Authorization = 'Bearer ' + accessToken
	* header Content-Type = 'application/json'
	When method get
	Then status 200
	* def filt = function(x){ return x.advisoryId == advisoryId }
    * def items = get response[*]
    * def revision = karate.filter(items, filt)[0].revision 

	# Logout
	Given url 'http://localhost:9000/realms/csaf/protocol/openid-connect/logout'
	* header Authorization = 'Bearer ' + accessToken
	* header Content-Type = 'application/x-www-form-urlencoded'
	* form field refresh_token = refreshToken
	* form field client_id = 'secvisogram'
	When method post
	* status 204
	