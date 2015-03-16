/**
 * Copyright 2015 IBM Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.ibmcloud.contest.phonebook;

import java.util.ArrayList;
import java.util.List;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.transaction.Status;
import javax.transaction.UserTransaction;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/phonebook")
/**
 * CRUD service for phonebook table. It uses REST Style
 *
 */
public class PhonebookServiceHandler {

    private final UserTransaction utx;
    private final EntityManager em;

    public PhonebookServiceHandler() {
        utx = getUserTransaction();
        em = getEm();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public PhonebookEntries queryPhonebook(@QueryParam("title") final String title,
            @QueryParam("firstname") final String firstname, @QueryParam("lastname") final String lastname) {

        final CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
        final CriteriaQuery<PhonebookEntry> criteriaQuery = criteriaBuilder.createQuery(PhonebookEntry.class);
        final Root<PhonebookEntry> entry = criteriaQuery.from(PhonebookEntry.class);
        final List<Predicate> predicates = new ArrayList<Predicate>();

        if (title != null) {
            predicates.add(criteriaBuilder.equal(entry.get("title"), title)); //$NON-NLS-1$
        }
        if (firstname != null) {
            predicates.add(criteriaBuilder.equal(entry.get("firstName"), firstname)); //$NON-NLS-1$
        }
        if (lastname != null) {
            predicates.add(criteriaBuilder.equal(entry.get("lastName"), lastname)); //$NON-NLS-1$
        }

        criteriaQuery.select(entry).where(predicates.toArray(new Predicate[] {}));
        final List<PhonebookEntry> entryList = em.createQuery(criteriaQuery).getResultList();

        final PhonebookEntries entries = new PhonebookEntries();
        entries.setEntries(entryList);
        return entries;

    }

    @GET
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public PhonebookEntry getEntry(@PathParam("id") final String id) {
        final Long queryId = Long.parseLong(id);
        final PhonebookEntry dbEntry = em.find(PhonebookEntry.class, queryId);
        if (dbEntry != null) {
            return dbEntry;
        } else {
            throw new NotFoundException();
        }

    }

    @GET
    @Path("/populate")
    @Produces(MediaType.APPLICATION_JSON)
    public PhonebookEntries populateDatabase() {
        List<PhonebookEntry> list = em.createQuery("SELECT t FROM PhonebookEntry t", PhonebookEntry.class) //$NON-NLS-1$
                .getResultList();
        if (list.size() == 0) {
            createSampleData();
            list = em.createQuery("SELECT t FROM PhonebookEntry t", PhonebookEntry.class).getResultList(); //$NON-NLS-1$
        }
        final PhonebookEntries entries = new PhonebookEntries();
        entries.setEntries(list);
        return entries;

    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public PhonebookEntry create(final PhonebookEntry entry) {
        try {
            utx.begin();
            em.persist(entry);
            utx.commit();
            return entry;
        } catch (final Exception e) {
            e.printStackTrace();
            throw new WebApplicationException();
        } finally {
            try {
                if (utx.getStatus() == Status.STATUS_ACTIVE) {
                    utx.rollback();
                }
            } catch (final Exception e) {
                e.printStackTrace();
            }
        }

    }

    @PUT
    @Path("{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public PhonebookEntry update(@PathParam("id") final String id, final PhonebookEntry entry) {
        final Long queryId = Long.parseLong(id);

        try {
            utx.begin();
            final PhonebookEntry dbEntry = em.find(PhonebookEntry.class, queryId);
            if (dbEntry == null) {
                throw new NotFoundException();
            }
            dbEntry.setTitle(entry.getTitle());
            dbEntry.setFirstName(entry.getFirstName());
            dbEntry.setLastName(entry.getLastName());
            dbEntry.setPhoneNumber(entry.getPhoneNumber());
            em.merge(dbEntry);
            utx.commit();
            return dbEntry;
        } catch (final Exception e) {
            e.printStackTrace();
            throw new WebApplicationException();
        }

    }

    @DELETE
    @Path("{id}")
    public Response deleteEntry(@PathParam("id") final String id) {
        final Long queryId = Long.parseLong(id);

        try {
            utx.begin();
            final PhonebookEntry dbEntry = em.find(PhonebookEntry.class, queryId);
            if (dbEntry == null) {
                throw new NotFoundException();
            }
            em.remove(dbEntry);
            utx.commit();
            return Response.ok().build();
        } catch (final Exception e) {
            e.printStackTrace();
            throw new WebApplicationException();
        }

    }

    private void createSampleData() {
        create(new PhonebookEntry("Mr", "Fred", "Jones", "01962 000000")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
        create(new PhonebookEntry("Mrs", "Jane", "Doe", "01962 000001")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
    }

    private UserTransaction getUserTransaction() {
        InitialContext ic;
        try {
            ic = new InitialContext();
            return (UserTransaction) ic.lookup("java:comp/UserTransaction"); //$NON-NLS-1$
        } catch (final NamingException e) {
            e.printStackTrace();
        }
        return null;
    }

    private EntityManager getEm() {
        InitialContext ic;
        try {
            ic = new InitialContext();
            return (EntityManager) ic.lookup("java:comp/env/openjpa-phonebook/entitymanager"); //$NON-NLS-1$
        } catch (final NamingException e) {
            e.printStackTrace();
        }
        return null;
    }

}
