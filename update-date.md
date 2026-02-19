```javascript
db.flights.updateMany(
  {}, // Filtro: applica a tutta la collection
  [
    // STEP 1: Creiamo dei campi temporanei per facilitare i calcoli
    {
      $set: {
        "_tmp_base_date": { $toDate: "$flight_info.schedule.date" },
        "_tmp_dep_int": { $toInt: "$flight_info.schedule.departure" },
        "_tmp_arr_int": { $toInt: { $toDouble: "$flight_info.schedule.arrival" } }
      }
    },
    // STEP 2: Calcoliamo il vero e proprio DateTime di partenza
    {
      $set: {
        "flight_info.schedule.departure_datetime": {
          $dateAdd: {
            startDate: {
              $dateAdd: {
                startDate: "$_tmp_base_date",
                unit: "hour",
                amount: { $floor: { $divide: ["$_tmp_dep_int", 100] } } // Estrae le ore
              }
            },
            unit: "minute",
            amount: { $mod: ["$_tmp_dep_int", 100] } // Estrae i minuti
          }
        }
      }
    },
    // STEP 3: Calcoliamo l'arrivo (risolvendo il salto della mezzanotte con $cond)
    {
      $set: {
        "flight_info.schedule.arrival_datetime": { // Correzione: cambiato per non sovrascrivere departure_datetime
          $dateAdd: {
            startDate: {
              $dateAdd: {
                // Implementazione della condizione "se l'arrivo è minore della partenza, aggiungi 1 giorno"
                startDate: {
                  $cond: {
                    if: { $lt: ["$_tmp_arr_int", "$_tmp_dep_int"] },
                    then: { $dateAdd: { startDate: "$_tmp_base_date", unit: "day", amount: 1 } },
                    else: "$_tmp_base_date"
                  }
                },
                unit: "hour",
                amount: { $floor: { $divide: ["$_tmp_arr_int", 100] } } // Estrae le ore di arrivo
              }
            },
            unit: "minute",
            amount: { $mod: ["$_tmp_arr_int", 100] } // Estrae i minuti di arrivo
          }
        }
      }
    },
    // STEP 4: Pulizia dei campi temporanei e di quelli vecchi non più necessari
    {
      $unset: [
        "_tmp_base_date", 
        "_tmp_dep_int",
        "_tmp_arr_int",
        // Rimuovi i commenti alle righe seguenti se vuoi cancellare i vecchi campi stringa
        "flight_info.schedule.departure",
        "flight_info.schedule.arrival",
        "flight_info.schedule.date"
      ]
    }
  ]
)
```

# update airports coordinates
```javascript
db.airports.bulkWrite([
  {
    updateOne: {
      filter: { _id: 'ECP' },
      update: { $set: { "location.coordinates": [-85.795602, 30.358241] } }
    }
  },
  {
    updateOne: {
      filter: { _id: 'PBG' },
      update: { $set: { "location.coordinates": [-73.468139, 44.650944] } }
    }
  },
  {
    updateOne: {
      filter: { _id: 'UST' },
      update: { $set: { "location.coordinates": [-81.339729, 29.959250] } }
    }
  }
]);
```

# indici

## airport

```javascript
db.airports.createIndex({ location: "2dsphere" });
```
