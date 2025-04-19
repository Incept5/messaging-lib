package org.incept5.example

import java.util.UUID

data class ExamplePayload(val msg: String, val count: Int, val id: UUID = UUID.randomUUID())
