# Espansione senza N+1

`GET /api/orders?_expand=customer` deve restituire ogni ordine con il suo cliente, come fa `json-server`. `Order` tiene solo `customerId` e un campo `@Transient Customer customer`: niente relazione JPA, quindi il cliente va caricato a mano.

## Il problema

Caricare il cliente ordine per ordine costa una query per riga: con `_limit=50` sono 51 query (1 per la pagina, 50 per i clienti).

## La scelta: una query batch

`OrderService` raccoglie gli ID distinti della pagina e li carica con un solo `findAllById`:

```java
// src/main/java/com/apexflow/service/OrderService.java — expandCustomer
Set<String> customerIds = orders.stream()
        .map(Order::getCustomerId)
        .filter(Objects::nonNull)
        .collect(Collectors.toSet());

Map<String, Customer> customerMap = customerRepository.findAllById(customerIds).stream()
        .collect(Collectors.toMap(Customer::getId, c -> c));

for (Order order : orders) {
    if (order.getCustomerId() != null) {
        order.setCustomer(customerMap.get(order.getCustomerId()));
    }
}
```

Qualunque sia la dimensione della pagina, le query sono due.

## Alternative scartate

- **`@ManyToOne` su `Order.customer`**: cambierebbe la forma del JSON e lo schema rispetto a `db.json`, che il frontend si aspetta con `customerId` piatto.
- **`JOIN FETCH`**: richiede la relazione JPA, quindi lo stesso problema.

## Stesso schema sui clienti

`GET /api/customers?_embed=customerStats` (e `_sort=customerStats.totalRevenue`) carica le statistiche della pagina con una sola `findByCustomerIdIn` e le raggruppa per cliente:

```java
// src/main/java/com/apexflow/service/CustomerService.java — embedStats
List<String> ids = customers.stream().map(Customer::getId).toList();
Map<String, List<CustomerStats>> statsByCustomer = new HashMap<>();
for (CustomerStats s : customerStatsRepository.findByCustomerIdIn(ids)) {
    statsByCustomer.computeIfAbsent(s.getCustomerId(), k -> new ArrayList<>()).add(s);
}
```
