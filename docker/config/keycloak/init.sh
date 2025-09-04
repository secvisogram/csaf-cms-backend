# This file is Free Software under the Apache-2.0 License
# without warranty, see README.md and LICENSES/Apache-2.0.txt for details.
#
# SPDX-License-Identifier: Apache-2.0
#
# SPDX-FileCopyrightText: 2024 German Federal Office for Information Security (BSI) <https://www.bsi.bund.de>
# Software-Engineering: 2024 Intevation GmbH <https://intevation.de>

#!/bin/bash

PATH=/opt/keycloak/bin:$PATH

adminuser=${CSAF_KEYCLOAK_ADMIN_USER}
adminpass=${CSAF_KEYCLOAK_ADMIN_PASSWORD}
client_hostname_url=${CSAF_KEYCLOAK_CLIENT_HOSTNAME_URL}
keycloak_url=${CSAF_KEYCLOAK_HOSTNAME_URL}
realm=${CSAF_REALM}
client_id=secvisogram


# log into the master realm with admin rights, token saved in ~/.keycloak/kcadm.config
echo "Login to keycloak $keycloak_url with user $adminuser"
kcadm.sh config credentials --server "$keycloak_url" --realm master --user "$adminuser" --password "$adminpass"

# Create the realm
echo "Create realm $realm"
kcadm.sh create realms --server "$keycloak_url" --realm master --set realm="$realm" --set enabled=true --output

# Get the client it
echo "Create client $client_id in realm $realm"
id=$(kcadm.sh create clients --server "$keycloak_url" --target-realm="$realm" --set clientId="$client_id" --set name="$client_id" --set enabled=true --id)

#kcadm.sh get realms/"$realm"/clients/$id --server "$keycloak_url"
echo "Configure basic config of client $client_id ($id) in realm $realm"    
kcadm.sh update clients/$id --server "$keycloak_url" --target-realm="$realm" \
	--set rootUrl=$client_hostname_url \
	--set "redirectUris=[\"$client_hostname_url/*\"]" \
	--set 'attributes={
    "oidc.ciba.grant.enabled" : "false",
    "post.logout.redirect.uris" : "+",
    "oauth2.device.authorization.grant.enabled" : "false",
    "backchannel.logout.session.required" : "true",
    "backchannel.logout.revoke.offline.tokens" : "false" }' \
	--set 'webOrigins=["*"]' \
	--set "adminUrl=$client_hostname_url/" \
	--set publicClient=true \
	--set standardFlowEnabled=true \
	--set directAccessGrantsEnabled=true \
	--set consentRequired=false

echo "Configure protocol mappers config of client $client_id ($id) in realm $realm"    
kcadm.sh update clients/$id --server "$keycloak_url" --target-realm="$realm" \
	--set 'protocolMappers=[ 
      {
      "name": "client_roles",
      "protocol": "openid-connect",
      "protocolMapper": "oidc-usermodel-client-role-mapper",
      "consentRequired": false,
      "config": {
         "introspection.token.claim": "true",
         "multivalued": "true",
         "userinfo.token.claim": "true",
         "id.token.claim": "true",
         "lightweight.claim": "false",
         "access.token.claim": "true",
         "claim.name": "groups",
         "jsonType.label": "String",
         "usermodel.clientRoleMapping.clientId": "secvisogram"
         }
      }
    ]'

# Create client roles
echo "Create client roles for client $client_id ($id) in realm $realm"
kcadm.sh create clients/$id/roles --server "$keycloak_url" --target-realm="$realm" --set name=registered --set "description=Registred user"
kcadm.sh create clients/$id/roles --server "$keycloak_url" --target-realm="$realm" --set name=author --set "description=author"
kcadm.sh create clients/$id/roles --server "$keycloak_url" --target-realm="$realm" --set name=editor --set "description=registred"
kcadm.sh create clients/$id/roles --server "$keycloak_url" --target-realm="$realm" --set name=publisher --set "description=registred"
kcadm.sh create clients/$id/roles --server "$keycloak_url" --target-realm="$realm" --set name=reviewer --set "description=registred"
kcadm.sh create clients/$id/roles --server "$keycloak_url" --target-realm="$realm" --set name=auditor --set "description=registred"

echo "Get client role ids for client $client_id ($id) in realm $realm"
#kcadm.sh get-roles --server "$keycloak_url" --target-realm="$realm" --cid "$id" --rolename author --fields id
client_role_author_id=$(kcadm.sh get-roles --server "$keycloak_url" --target-realm="$realm" --cid "$id" --rolename author    --fields id | grep -o '"id" *: *"[^"]*"' | sed 's/.*: *"\([^"]*\)".*/\1/')
client_role_editor_id=$(kcadm.sh get-roles --server "$keycloak_url" --target-realm="$realm" --cid "$id" --rolename editor    --fields id | grep -o '"id" *: *"[^"]*"' | sed 's/.*: *"\([^"]*\)".*/\1/')
client_role_publisher_id=$(kcadm.sh get-roles --server "$keycloak_url" --target-realm="$realm" --cid "$id" --rolename publisher --fields id | grep -o '"id" *: *"[^"]*"' | sed 's/.*: *"\([^"]*\)".*/\1/')
client_role_reviewer_id=$(kcadm.sh get-roles --server "$keycloak_url" --target-realm="$realm" --cid "$id" --rolename reviewer  --fields id | grep -o '"id" *: *"[^"]*"' | sed 's/.*: *"\([^"]*\)".*/\1/')
client_role_auditor_id=$(kcadm.sh get-roles --server "$keycloak_url" --target-realm="$realm" --cid "$id" --rolename auditor   --fields id | grep -o '"id" *: *"[^"]*"' | sed 's/.*: *"\([^"]*\)".*/\1/')

echo "Assign composite client roles for client $client_id ($id) in realm $realm"
kcadm.sh add-roles --server "$keycloak_url" --target-realm="$realm" --cid "$id" --rid $client_role_author_id    --rolename registered
kcadm.sh add-roles --server "$keycloak_url" --target-realm="$realm" --cid "$id" --rid $client_role_reviewer_id  --rolename registered
kcadm.sh add-roles --server "$keycloak_url" --target-realm="$realm" --cid "$id" --rid $client_role_editor_id    --rolename author
kcadm.sh add-roles --server "$keycloak_url" --target-realm="$realm" --cid "$id" --rid $client_role_publisher_id --rolename editor

#Create test users
echo "Create test users in realm $realm"
user_registered_id=$(kcadm.sh create users --server "$keycloak_url" --target-realm="$realm" -s username=registered -s enabled=true --id)
user_author_id=$(kcadm.sh create users --server "$keycloak_url" --target-realm="$realm" -s username=author -s enabled=true --id)
user_editor_id=$(kcadm.sh create users --server "$keycloak_url" --target-realm="$realm" -s username=editor -s enabled=true --id)
user_publisher_id=$(kcadm.sh create users --server "$keycloak_url" --target-realm="$realm" -s username=publisher -s enabled=true --id)
user_reviewer_id=$(kcadm.sh create users --server "$keycloak_url" --target-realm="$realm" -s username=reviewer -s enabled=true --id)
user_auditor_id=$(kcadm.sh create users --server "$keycloak_url" --target-realm="$realm" -s username=auditor -s enabled=true --id)
user_all_id=$(kcadm.sh create users --server "$keycloak_url" --target-realm="$realm" -s username=all -s enabled=true --id)
user_none_id=$(kcadm.sh create users --server "$keycloak_url" --target-realm="$realm" -s username=none -s enabled=true --id)

kcadm.sh update users/$user_registered_id --server "$keycloak_url" --target-realm="$realm" -s 'firstName=registered' -s 'lastName=r' -s 'email=registered@example.com'
kcadm.sh update users/$user_author_id     --server "$keycloak_url" --target-realm="$realm" -s 'firstName=author' -s 'lastName=a' -s 'email=author@example.com'
kcadm.sh update users/$user_editor_id     --server "$keycloak_url" --target-realm="$realm" -s 'firstName=editor' -s 'lastName=e' -s 'email=editor@example.com'
kcadm.sh update users/$user_publisher_id  --server "$keycloak_url" --target-realm="$realm" -s 'firstName=publisher' -s 'lastName=p' -s 'email=publisher@example.com'
kcadm.sh update users/$user_reviewer_id   --server "$keycloak_url" --target-realm="$realm" -s 'firstName=reviewer' -s 'lastName=r' -s 'email=reviewer@example.com'
kcadm.sh update users/$user_auditor_id    --server "$keycloak_url" --target-realm="$realm" -s 'firstName=auditor' -s 'lastName=a' -s 'email=auditor@example.com'
kcadm.sh update users/$user_all_id        --server "$keycloak_url" --target-realm="$realm" -s 'firstName=all' -s 'lastName=all' -s 'email=all@example.com'
kcadm.sh update users/$user_none_id       --server "$keycloak_url" --target-realm="$realm" -s 'firstName=none' -s 'lastName=none' -s 'email=none@example.com'

#Add user to client roles
echo "Assign client roles to test users in realm $realm"
kcadm.sh add-roles --server "$keycloak_url" --target-realm="$realm" --uusername registered --cid "$id" --rolename registered 
kcadm.sh add-roles --server "$keycloak_url" --target-realm="$realm" --uusername author     --cid "$id" --rolename author     
kcadm.sh add-roles --server "$keycloak_url" --target-realm="$realm" --uusername editor     --cid "$id" --rolename editor     
kcadm.sh add-roles --server "$keycloak_url" --target-realm="$realm" --uusername publisher  --cid "$id" --rolename publisher  
kcadm.sh add-roles --server "$keycloak_url" --target-realm="$realm" --uusername reviewer   --cid "$id" --rolename reviewer   
kcadm.sh add-roles --server "$keycloak_url" --target-realm="$realm" --uusername auditor    --cid "$id" --rolename auditor  
kcadm.sh add-roles --server "$keycloak_url" --target-realm="$realm" --uusername all        --cid "$id" --rolename publisher --rolename reviewer --rolename auditor --rolename auditor

#Set user password
echo "Set password for test users in realm $realm"
kcadm.sh set-password --server "$keycloak_url" --target-realm="$realm" --username registered --new-password registered 
kcadm.sh set-password --server "$keycloak_url" --target-realm="$realm" --username author --new-password author
kcadm.sh set-password --server "$keycloak_url" --target-realm="$realm" --username editor --new-password editor
kcadm.sh set-password --server "$keycloak_url" --target-realm="$realm" --username publisher --new-password publisher
kcadm.sh set-password --server "$keycloak_url" --target-realm="$realm" --username reviewer --new-password reviewer
kcadm.sh set-password --server "$keycloak_url" --target-realm="$realm" --username auditor --new-password auditor
kcadm.sh set-password --server "$keycloak_url" --target-realm="$realm" --username all --new-password all
kcadm.sh set-password --server "$keycloak_url" --target-realm="$realm" --username none --new-password none
