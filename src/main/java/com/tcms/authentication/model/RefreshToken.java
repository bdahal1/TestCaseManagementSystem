package com.tcms.authentication.model;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.lang.NonNull;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "refresh_token")
public class RefreshToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ref_id")
    @NonNull
    private int id;
    @Column(name = "username")
    @NonNull
    private String userName;
    @Column(name = "token")
    @NonNull
    private String token;
    @Column(name = "expiry_date")
    @NonNull
    private Instant expiryDate;

    @Override
    public String toString() {
        return "RefreshToken{" +
                "id=" + id +
                ", userName=" + userName +
                ", token='" + token + '\'' +
                ", expiryDate=" + expiryDate +
                '}';
    }
}