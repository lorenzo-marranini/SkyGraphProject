# Sort routes by mean airtime

```javascript
db.flights.aggregate([
  {
    $match: {
      "stats.air_time": { $ne: null, $type: "number" }
    }
  },
  {
    $group: {
      _id: {
        origin: "$route.origin.airport_name",
        destination: "$route.destination.airport_name"
      },
      mean_airtime: { $avg: "$stats.air_time" }
    }
  },
  {
    $sort: { mean_airtime: -1 }
  },
  {
    $limit: 1
  }
])
```

# Sort airlines by number of divertion

```javascript
db.flights.aggregate([
  {
    $group: {
      _id: "$flight_info.airline_name",
      voli_dirottati: { $sum: "$stats.diverted" },
      voli_cancellati: { $sum: "$stats.cancelled" }
    }
  },
  {
    $sort: { voli_dirottati: 1 }
  }
])
```

# Generate airline report

```javascript
db.flights.aggregate([
  {
    $group: {
      _id: "$flight_info.airline_name",
      distanza_totale: { $sum: "$route.distance" },
      ritardo_medio: { $avg: "$stats.tot_delay" },
      numero_voli: { $sum: 1 }
    }
  },
  { $sort: { numero_voli: -1 } }
])
```

# Mean route distance per specific airline

```javascript
db.flights.aggregate([
  { $match: { "flight_info.airline_code": "DL" } },
  {
    $group: {
      _id: "$flight_info.airline_name",
      distanza_media_per_volo: { $avg: "$route.distance" }
    }
  }
])
```

# Most common route

```javascript
db.flights.aggregate([
  {
    $group: {
      _id: { origin: "$route.origin.iata", dest: "$route.destination.iata" },
      totale_voli: { $sum: 1 },
      voli_cancellati: { $sum: "$stats.cancelled" },
      voli_dirottati: { $sum: "$stats.diverted"},
    }
  }, {
    $sort: { totale_voli: -1}
  }
])
```

# Delay by day of the week

```javascript
db.flights.aggregate([
  {
    $project: {
      data_volo: { $toDate: "$flight_info.date" },
      ritardo: "$stats.tot_delay"
    }
  },
  {
    $group: {
      _id: { $dayOfWeek: "$data_volo" },
      ritardo_medio: { $avg: "$ritardo" },
      numero_voli: { $sum: 1 }
    }
  },
  { $sort: { ritardo_medio: -1 } }
])
```
