import DocumentEntity from "DocumentEntity.mjs";

const PRODUCT_STATUS_HEADER = `
<thead>
  <tr>
    <th>Product</th>
    <th>CVSS-Vector</th>
    <th>CVSS Base Score</th>
  </tr>
</thead>`

const PRODUCT_STATUS_ROW = `
<tr>
  <td>{{name}}</td>
  <td>{{vectorString}}</td>
  <td>{{baseScore}}</td>
</tr>`

const REMEDIATION = `
<h5>{{#replaceUnderscores}}{{#upperCase}}{{category}}{{/upperCase}}{{/replaceUnderscores}}{{#date}} ({{.}}){{/date}}</h5>
<p>{{details}}</p>
{{#product_ids.length}}
  <h6>For products:</h6>
  <ul>
  {{#product_ids}}
    <li>{{name}}</li>
  {{/product_ids}}
  </ul>
{{/product_ids.length}}
{{#group_ids.length}}
  <h6>For groups:</h6>
  <ul>
  {{#group_ids}}
   <li>{{name}}</li>
  {{/group_ids}}
  </ul>
{{/group_ids.length}}        
{{#url}}<p><a href={{.}}>{{.}}</a></p>{{/url}}
{{#entitlements}}
  <p>{{.}}</p>
{{/entitlements}}
{{#restart_required}}
  Restart required: <b>{{category}}</b>
  <p>{{details}}</p>
{{/restart_required}}`

const THREAT = `
<h5>{{#replaceUnderscores}}{{#upperCase}}{{category}}{{/upperCase}}{{/replaceUnderscores}}{{#date}} ({{.}}){{/date}}</h5>
<p>{{details}}</p>
{{#product_ids.length}}
  <h6>For products:</h6>
  <ul>
  {{#product_ids}}
    <li>{{name}}</li>
  {{/product_ids}}
  </ul>
{{/product_ids.length}}
{{#group_ids.length}}
  <h6>For groups:</h6>
  <ul>
  {{#group_ids}}
   <li>{{name}}</li>
  {{/group_ids}}
  </ul>
{{/group_ids.length}}`

const VULNERABILITY_NOTE = `
{{#title}}<b>{{.}}</b>{{/title}}{{#audience}} ({{.}}){{/audience}}
{{#text}}<p>{{text}}</p>{{/text}}`

const DOCUMENT_NOTE = `
{{#title}}<h2>{{.}}</h2>{{/title}}
{{#audience}}<small>{{.}}</small>{{/audience}}
{{#text}}<p>{{text}}</p>{{/text}}`
const ACKNOWLEDGEMENT = `
{{#.}}
  <li>{{#removeTrailingComma}}{{#names}}{{.}}, {{/names}}{{/removeTrailingComma}}{{#organization}}{{#names.length}} from {{/names.length}}{{.}} {{/organization}}{{#summary}} for {{.}}{{/summary}}{{#urls.length}} (see: {{#removeTrailingComma}}{{#urls}}<a href={{.}}>{{.}}</a>, {{/urls}}{{/removeTrailingComma}}){{/urls.length}}</li>  
{{/.}}`

const REFERENCE = `
{{#.}}
  <li>{{summary}} {{#category}} ({{#replaceUnderscores}}{{.}}{{/replaceUnderscores}}){{/category}}{{#url}}: <a href={{.}}>{{.}}</a>{{/url}}</li>
{{/.}}`


export function renderWithMustache(template, json, logo) {
    const obj = JSON.parse(json);
    if (logo) {
        obj['logo'] = JSON.parse(logo);
    }
    print(JSON.stringify(obj, null, 2))
    const documentEntity = new DocumentEntity();
    const doc = documentEntity.preview(obj);
    print(doc.document.title)
    return Mustache.render(template, obj, {
        product_status_header: PRODUCT_STATUS_HEADER,
        product_status_row: PRODUCT_STATUS_ROW,
        remediation: REMEDIATION,
        threat: THREAT,
        vulnerability_note: VULNERABILITY_NOTE,
        document_note: DOCUMENT_NOTE,
        acknowledgment: ACKNOWLEDGEMENT,
        reference: REFERENCE,
    });
}
