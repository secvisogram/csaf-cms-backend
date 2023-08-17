Feature: Test export of documents

  Background:
      * def authUrl = 'http://localhost:9000'
      * def loginPath = '/realms/csaf/protocol/openid-connect/token'
      * def logoutPath = '/realms/csaf/protocol/openid-connect/logout'
      
      * def restUrl = 'http://localhost:8081'
      * def apiBase = '/api/v1/advisories'
      
      * def username = 'all'
      * def password = 'all'

  Scenario: Export all formats and store response in folder ./target/
      The first document in the list will be exported in all available 
      formats.
  
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

    #Get advisory list and store first advisory id
	Given url 'http://localhost:8081' 
	* path apiBase
	* header Authorization = 'Bearer ' + accessToken
	* header Content-Type = 'application/json'
	When method get
	Then status 200
	* def advisoryId = response[0].advisoryId
	
	# Download advisory as JSON
	* def format = 'JSON'
	Given path apiBase + '/' + advisoryId + '/csaf'
	* header Authorization = 'Bearer ' + accessToken
	* header Content-Type = 'application/json'
	* param format = format
	When method get
	Then status 200
	* karate.write(response, 'advisory.json') 

	# Download advisory as HTML
	* def format = 'HTML'
	Given path apiBase + '/' + advisoryId + '/csaf'
	* header Authorization = 'Bearer ' + accessToken
	* header Content-Type = 'application/json'
	* param format = format
	When method get
	Then status 200
	* karate.write(response, 'advisory.html')

	# Download advisory as PDF
	* def format = 'PDF'
	Given path apiBase + '/' + advisoryId + '/csaf'
	* header Authorization = 'Bearer ' + accessToken
	* header Content-Type = 'application/json'
	* param format = format
	When method get
	Then status 200
	* karate.write(response, 'advisory.pdf')

	# Download advisory as Markdown
	* def format = 'Markdown'
	Given path apiBase + '/' + advisoryId + '/csaf'
	* header Authorization = 'Bearer ' + accessToken
	* header Content-Type = 'application/json'
	* param format = format
	When method get
	Then status 200
	* karate.write(response, 'advisory.md')

	# Logout
	Given url 'http://localhost:9000/realms/csaf/protocol/openid-connect/logout'
	* header Authorization = 'Bearer ' + accessToken
	* header Content-Type = 'application/x-www-form-urlencoded'
	* form field refresh_token = refreshToken
	* form field client_id = 'secvisogram'
	When method post
	* status 204
	