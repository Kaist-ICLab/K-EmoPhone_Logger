# Data Collector for Smartphone Android (< v.7.0; API 21)

## Data Types

### Using System-level BroadcastReceiver  
- /orm/entities/BatteryEntity
- /orm/entities/ConnectivityEntity
- /orm/entities/DeviceEventEntity

### Using JobService (1 hour period)
- /orm/entities/MediaEntity
- /orm/entities/CallLogEntity
- /orm/entities/MessageEntity

### Using AccessibilityService
- /orm/entities/InteractionEntity

### Using NotificationListenerService
- /orm/entities/NotificationEntity

### Using Service
- /orm/entities/PhysicalActivityEntity
- /orm/entities/DataTrafficEntity
- /orm/entities/RecordEntity
#### Activated considering PhysicalActivityEntity (when 30 seconds Still state)
- /orm/entities/WifiEntity
- /orm/entities/LocationEntity

### Using Unlock
- /orm/entities/SelfieEntity