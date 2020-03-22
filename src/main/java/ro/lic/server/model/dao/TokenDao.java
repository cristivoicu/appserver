package ro.lic.server.model.dao;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import ro.lic.server.model.tables.Token;

@Repository
public interface TokenDao extends CrudRepository<Token, Long> {
}
