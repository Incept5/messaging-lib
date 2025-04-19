package org.incept5.example

import jakarta.inject.Singleton
import java.sql.ResultSet
import java.util.*
import javax.sql.DataSource

@Singleton
class ExampleDataRepository(private val dataSource: DataSource ) {

    private val tableName = "example.example_data"


    fun findById(id: UUID): ExampleData? {
        dataSource.connection.use { conn ->
            conn.prepareStatement("SELECT * FROM $tableName WHERE id = ?").use { stmt ->
                stmt.setObject(1, id)
                val rs = stmt.executeQuery()
                if (rs.next()) return mapRow(rs)
            }
        }
        return null
    }

    fun save(file: ExampleData): ExampleData {
        dataSource.connection.use { conn ->
            conn.prepareStatement("INSERT INTO $tableName (id) VALUES (?)").use { stmt ->
                stmt.setObject(1, file.id)
                stmt.executeUpdate()
            }
        }
        return file
    }

    private fun mapRow(rs: ResultSet): ExampleData {
        return ExampleData(
            rs.getObject("id", UUID::class.java)
        )
    }
}
