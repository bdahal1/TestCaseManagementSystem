package com.tcms.services;

import tools.jackson.databind.ObjectMapper;
import com.tcms.models.Users;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class UserSerializationTest {
    @Test void passwordIsNeverSerializedOrErased() throws Exception {
        Users user = new Users(); user.setPassword("stored-hash");
        ObjectMapper mapper = tools.jackson.databind.json.JsonMapper.builder().build();
        assertThat(mapper.readTree(mapper.writeValueAsString(user)).has("password")).isFalse();
        assertThat(user.getPassword()).isEqualTo("stored-hash");
        assertThat(mapper.readValue("{\"password\":\"input\"}", Users.class).getPassword()).isEqualTo("input");
    }
}
