<template>
  <div v-if="fetching">
    Loading...
  </div>
  <div v-else-if="error">
    Oh no... {{ error }}
  </div>
  <div v-else>
    <p>
      Book sources are there: <a href="https://github.com/michael-simons/goodreads">michael-simons/goodreads</a>,
      a GraphQL UI instance is <a
        href="/q/graphql-ui/?query=%7B%0A%20%20books%20%7B%0A%20%20%20%20title%0A%20%20%20%20authors%20%7B%0A%20%20%20%20%20%20name%0A%20%20%20%20%7D%0A%20%20%7D%0A%7D%0A">here</a>
      and last but not least, the sources of this applications are on GitHub as well: <a
        href="https://github.com/michael-simons/neo4j-aura-quarkus-graphql">michael-simons/neo4j-aura-quarkus-graphql</a>.
    </p>
    <label>Filter by title or author</label> <input type="text" placeholder="Filter by title or author."
                                                    v-model="filter" width="256"/>&#160;
    <label>Unread only</label> <input type="checkbox" v-model="unreadOnly"/>
    <br/>
    <p>In case new books have been added to the <em>goodreads</em> repository, they can be fetch here:
      <button v-on:click="fetchNewBooks">Fetch new books</button>
    </p>
    <br/>
    <table v-if="data">
      <thead>
        <tr>
          <th>Title</th>
          <th>Author(s)</th>
        </tr>
      </thead>
      <tfoot/>
      <tbody>
        <tr v-for="book in books" v-bind:key="book.id">
          <td>{{ book.title }}</td>
          <td>{{ book.authors }}</td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<script>
import {useMutation, useQuery} from '@urql/vue';
import {ref} from 'vue';

export default {

  setup() {

    const unreadOnly = ref(false);
    const result = useQuery({
      query: `
       query ($unreadOnly: Boolean!) {
        books ( unreadOnly: $unreadOnly) {
          title
          authors {
            name
          }
        }
      }
      `,
      variables: {unreadOnly}
    });

    const updateBooksResult = useMutation(`
      mutation ($unreadOnly: Boolean!) {
        updateBooks (unreadOnly: $unreadOnly) {
          title
          state
          authors {
            name
          }
        }
      }
    `);

    return {
      unreadOnly,
      fetchNewBooks: function () {
        updateBooksResult.executeMutation({'unreadOnly': unreadOnly.value}).then(
            result => {
              this.data.books = result.data.updateBooks
            })
      },
      fetching: result.fetching,
      data: result.data,
      error: result.error
    };
  },
  data() {
    return {
      filter: ''
    }
  },
  computed: {
    books() {
      const searchTerm = this.filter.toLowerCase();
      return this.data.books
          .map(b => {
            return {
              id: b.id,
              title: b.title,
              authors: b.authors.map(a => a.name).join(", ")
            }
          })
          .filter(row => {
            return searchTerm == '' || row.title.toLowerCase().includes(searchTerm) || row.authors.toLowerCase().includes(searchTerm)
          })
          .sort((b1, b2) => {
            var result = b1.title.localeCompare(b2.title);
            if (result == 0) {
              result = b1.authors.localeCompare(b2.authors);
            }
            return result;
          })
    }
  },
};
</script>
