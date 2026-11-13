package com.example.demo.audit;

import java.sql.Connection;
import java.sql.Statement;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuditSchemaTests {
    @Test
    void initializerKeepsSessionAuditFunctionInOneStatement() throws Exception {
        Connection connection = mock(Connection.class);
        Statement statement = mock(Statement.class);
        when(connection.createStatement()).thenReturn(statement);
        when(statement.getUpdateCount()).thenReturn(-1);
        ScriptUtils.executeSqlScript(connection, new ClassPathResource("schema.sql"));
        var commands = ArgumentCaptor.forClass(String.class);
        verify(statement, atLeastOnce()).execute(commands.capture());
        var functions = commands.getAllValues().stream()
                .filter(sql -> sql.startsWith("CREATE OR REPLACE FUNCTION stamp_session_audit")).toList();
        assertEquals(1, functions.size());
        assertTrue(functions.getFirst().contains("RETURN NEW;"));
        assertTrue(functions.getFirst().contains("NEW.created_by := OLD.created_by;"));
        for (String table : new String[]{"user_roles", "spring_session", "spring_session_attributes"}) {
            assertTrue(commands.getAllValues().stream().anyMatch(sql ->
                    sql.startsWith("ALTER TABLE IF EXISTS " + table + " ")
                            && sql.contains("created_by") && sql.contains("updated_by")));
        }
    }
}
