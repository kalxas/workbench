package eu.slipo.workbench.web.repository;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TypedQuery;

import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import eu.slipo.workbench.common.domain.AccountEntity;
import eu.slipo.workbench.common.model.EnumRole;
import eu.slipo.workbench.common.model.QueryResultPage;
import eu.slipo.workbench.common.model.user.Account;
import eu.slipo.workbench.web.model.admin.AccountQuery;

@Repository()
public class DefaultAccountRepository implements AccountRepository {

    @PersistenceContext(unitName = "default")
    private EntityManager entityManager;

    @Override
    public QueryResultPage<Account> query(AccountQuery query, PageRequest pageReq) {
        // Check query parameters
        if (pageReq == null) {
            pageReq = new PageRequest(0, 10);
        }

        String qlString = "";

        // Resolve filters

        List<String> filters = new ArrayList<>();
        if (query != null) {
            if (!StringUtils.isEmpty(query.getUserName())) {
                filters.add("(a.username like :userName)");
            }
        }

        // Count records
        qlString = "select count(a.id) from Account a ";
        if (!filters.isEmpty()) {
            qlString += " where " + StringUtils.join(filters, " and ");
        }

        Integer count;
        TypedQuery<Number> countQuery = entityManager.createQuery(qlString, Number.class);
        if (query != null) {
            setFindParameters(query, countQuery);
        }
        count = countQuery.getSingleResult().intValue();

        // Load records
        qlString = "select a from Account a ";
        if (!filters.isEmpty()) {
            qlString += " where " + StringUtils.join(filters, " and ");
        }
        qlString += " order by a.username, a.id ";

        TypedQuery<AccountEntity> selectQuery = entityManager.createQuery(qlString, AccountEntity.class);
        if (query != null) {
            setFindParameters(query, selectQuery);
        }

        selectQuery.setFirstResult(pageReq.getOffset());
        selectQuery.setMaxResults(pageReq.getPageSize());

        List<Account> records = selectQuery.getResultList().stream()
            .map(e -> e.toDto())
            .collect(Collectors.toList());

        return new QueryResultPage<Account>(records, pageReq, count);
    }

    @Override
    public Account findOne(int id) {
        AccountEntity account = this.findEntity(id);

        return (account == null ? null : account.toDto());
    }

    @Override
    public void update(int updatedBy, Account account) {
        AccountEntity entity = this.findEntity(account.getId());
        AccountEntity grantedBy = this.findEntity(updatedBy);

        entity.setName(account.getGivenName(), account.getFamilyName());

        // Revoke roles
        for (Iterator<EnumRole> i = entity.getRoles().iterator(); i.hasNext();) {
            EnumRole role = i.next();
            if(!account.getRoles().contains(role)) {
                entity.revoke(role);
            }
        }

        // Grant roles
        account.getRoles().stream().forEach(r-> {
            if(!entity.hasRole(r)) {
                entity.grant(r, grantedBy);
            }
        });
    }

    private AccountEntity findEntity(int id) {
        String qlString = "select a from Account a where a.id = :id ";

        List<AccountEntity> accounts = entityManager
            .createQuery(qlString, AccountEntity.class)
            .setParameter("id", id)
            .setMaxResults(1)
            .getResultList();

        return (accounts.isEmpty() ? null : accounts.get(0));
    }

    private void setFindParameters(AccountQuery eventQuery, Query query) {
        if (!StringUtils.isEmpty(eventQuery.getUserName())) {
            query.setParameter("userName", "%" + eventQuery.getUserName() + "%");
        }
    }

}
