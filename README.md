# Mermaid ClassDiagram Generator
Generates Mermaid class diagrams from JsonSchema files


## Examples

This README includes progressive examples showing input JSON Schemas (both JSON and YAML) and the expected Mermaid class diagram output. Required fields are prefixed with `+`. Optional fields have no prefix.

### 1) Simple: Person (JSON)
Input (person.schema.json):

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "title": "Person",
  "type": "object",
  "properties": {
    "id": { "type": "integer" },
    "name": { "type": "string" },
    "email": { "type": "string", "format": "email" },
    "isActive": { "type": "boolean", "default": true }
  },
  "required": ["id", "name"]
}
```

Generated Mermaid:

```mermaid
classDiagram
  class Person {
    +Integer id
    +String name
    String email
    Boolean isActive
  }
```

### 2) Simple: Person (YAML)
Input (person.schema.yaml):

```yaml
$schema: "http://json-schema.org/draft-07/schema#"
title: Person
type: object
properties:
  id:
    type: integer
  name:
    type: string
  tags:
    type: array
    items:
      type: string
required:
  - id
  - name
```

Generated Mermaid:

```mermaid
classDiagram
  class Person {
    +Integer id
    +String name
    String[] tags
  }
```

### 3) Nested Objects and Arrays
Input (order.schema.json):

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "title": "Order",
  "type": "object",
  "properties": {
    "orderId": { "type": "string" },
    "customer": {
      "type": "object",
      "properties": {
        "customerId": { "type": "string" },
        "name": { "type": "string" }
      },
      "required": ["customerId"]
    },
    "items": {
      "type": "array",
      "items": {
        "type": "object",
        "properties": {
          "productId": { "type": "string" },
          "quantity": { "type": "integer" }
        },
        "required": ["productId"]
      }
    }
  },
  "required": ["orderId", "customer", "items"]
}
```

Generated Mermaid:

```mermaid
classDiagram
  class Order {
    +String orderId
  }
  class Customer {
    +String customerId
    String name
  }
  class OrderItem {
    +String productId
    Integer quantity
  }

  Order "1" --> "1" Customer : customer
  Order "1" --> "*" OrderItem : items
```

### 4) References ($ref), Reuse and Enums
Input (product-catalog.schema.yaml):

```yaml
$schema: http://json-schema.org/draft-07/schema#
title: ProductCatalog
type: object
definitions:
  money:
    type: object
    properties:
      currency:
        type: string
        enum: [USD, EUR, GBP]
      amount:
        type: number
    required: [currency, amount]
  product:
    type: object
    properties:
      id:
        type: string
      name:
        type: string
      price:
        $ref: '#/definitions/money'
    required: [id, name, price]
properties:
  products:
    type: array
    items:
      $ref: '#/definitions/product'
```

Generated Mermaid:

```mermaid
classDiagram
  class ProductCatalog {
  }
  class Product {
    +String id
    +String name
    +Money price
  }
  class Money {
    +String currency
    +Number amount
  }

  ProductCatalog "1" --> "*" Product : products
  Product o-- Money : price
```

### 5) Complex: Composition (allOf, anyOf, oneOf), additionalProperties, patternProperties
Input (complex.schema.json):

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "title": "ComplexExample",
  "type": "object",
  "properties": {
    "id": { "type": "string" },
    "metadata": {
      "type": "object",
      "additionalProperties": { "type": "string" }
    },
    "attributes": {
      "type": "object",
      "patternProperties": {
        "^attr_": { "type": "number" }
      }
    },
    "shipment": {
      "allOf": [
        { "$ref": "#/definitions/address" },
        { "type": "object", "properties": { "eta": { "type": "string", "format": "date-time" } } }
      ]
    },
    "paymentMethod": {
      "oneOf": [
        { "$ref": "#/definitions/card" },
        { "$ref": "#/definitions/paypal" }
      ]
    }
  },
  "definitions": {
    "address": {
      "type": "object",
      "properties": {
        "street": { "type": "string" },
        "city": { "type": "string" }
      }
    },
    "card": {
      "type": "object",
      "properties": {
        "cardNumber": { "type": "string" }
      }
    },
    "paypal": {
      "type": "object",
      "properties": {
        "accountEmail": { "type": "string", "format": "email" }
      }
    }
  }
}
```

Generated Mermaid:

```mermaid
classDiagram
  class ComplexExample {
    String id
    Map<String,String> metadata
  }
  class Address {
    String street
    String city
  }
  class Card {
    String cardNumber
  }
  class Paypal {
    String accountEmail
  }

  ComplexExample "1" --> "1" Address : shipment
  ComplexExample "1" --> "1" Card : paymentMethod (oneOf)
  ComplexExample "1" --> "1" Paypal : paymentMethod (oneOf)
```

### 6) Inheritance with `extends`
Input (parent.schema.yaml):

```yaml
$id: parent.schema.yaml
$schema: https://json-schema.org/draft/2020-12/schema
title: Parent
properties:
  parentField:
    type: string
```

Input (child.schema.yaml):

```yaml
$id: child.schema.yaml
$schema: https://json-schema.org/draft/2020-12/schema
title: Child
extends:
  $ref: parent.schema.yaml
properties:
  childField:
    type: integer
```

Generated Mermaid:

```mermaid
classDiagram
  class Parent {
    String parentField
  }
  class Child {
    Integer childField
  }
  Parent <|-- Child
```

### Tips for reading these examples
- `+` indicates a required field.
- Arrays may be shown as X[] or as relationships with multiplicity "*".
- Inline anonymous objects are often pulled out into named classes by the generator.
- `$ref` leads to class reuse and may produce aggregation (`o--`) or association (`-->`).


## Usage

See the project scripts in `build/scripts` or the generated CLI in `build/scripts/jsonschema-to-mermaid` for how to run this generator against a schema file and get Mermaid markdown as output.


## Contributing examples
If you add additional examples, please include:
- the input schema file (JSON or YAML)
- the expected Mermaid markdown output
- a short note explaining noteworthy mapping decisions (e.g., how oneOf should be shown)
