# Offline delivery address directory

`delivery-addresses.tsv` contains province, city/municipality, barangay and operational delivery point, loaded on startup without network calls. Keep `src` on the runtime classpath, or package `data/` inside the application JAR.

Source: [PSGC API](https://psgc.gitlab.io/) province, cities-municipalities and barangay endpoints, downloaded October 13, 2026. This is a bundled snapshot, not a guarantee of the latest administrative changes. Official geographic classifications are maintained by the [Philippine Statistics Authority](https://psa.gov.ph/classifications-api/psgc). Province/city/barangay combinations are validated against the same file in the UI and backend.

Operational coverage defaults:

- CENTRAL: Central Luzon (Region III) plus Metro Manila (NCR).
- NORTHERN: Ilocos Region (I), Cagayan Valley (II), Cordillera Administrative Region (CAR).
- SOUTHERN: CALABARZON (IV-A) and Bicol (V).

These are business delivery zones. NCR is not administratively part of Central Luzon. MIMAROPA, Visayas and Mindanao are excluded from this initial coverage. To change coverage, update the delivery_point column consistently and include newly covered localities.

The warehouse is now based in Pasay. `delivery-coordinates.tsv` maps all 698 covered cities/municipalities to GeoNames coordinates for estimated straight-line distance ranking. Source: [GeoNames Philippines extract](https://download.geonames.org/export/dump/PH.zip), downloaded October 15, 2026, [CC BY 4.0](https://creativecommons.org/licenses/by/4.0/). Preserve attribution when redistributing. The Pasay origin uses approximate coordinates from [Pasay City Government](https://pasay.gov.ph/ating-lungsod/city-profile/). Estimates do not represent road routing, traffic, or street-level distance. The rebuild script is `scripts/build-delivery-coordinates.py`; it requires PH.txt and admin2Codes.txt in ignored `bin/geodata`.

Legacy free-text delivery addresses are left unassigned. Staff must review them through View details > Review delivery address before dispatch. New orders always require valid structured addresses, except pickup orders, which use the Central main hub.
