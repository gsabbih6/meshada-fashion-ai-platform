package com.sabbih.meshadacoreservice.social;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SocialCredentialsRepository extends JpaRepository<SocialCredentials, String> {
}
