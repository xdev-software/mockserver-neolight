# Comparison of similar frameworks

This list contains a comparison with the most popular open source [API simulation tools](https://en.wikipedia.org/wiki/Comparison_of_API_simulation_tools)

<details><summary>Chosen criteria</summary>

| Criteria | Reason |
| --- | --- |
| Mocking in IntegrationTests | Gives an overview how this is done and describes how straightforward this can be integrated into (Java) code |
| Modularity | Monolithic/Non-modular packaging causes bloat and problems such as <ul><li>Possible introduction of vulnerabilities</li><li>Dependency conflicts</li><li>Performance degeneration</li><li>Confusion (which classes/methods need to be used)</li></ul>
| Testcontainers Integration | Testcontainers is the defacto standard for running integration tests. It utilizes containers, is highly flexible and has widespread use in the Java ecosystem. |
| Activity & Support | Unmaintained software won't get fixes for incompatibilities, issues, vulnerabilities, ... |

</details>

<table>
    <tr>
        <th>Tool</th>
        <th>Mocking in IntegrationTests</th>
        <th>Modularity</th>
        <th>Testcontainers Integration</th>
        <th>Activity & Support</th>
    </tr>
    <tr>
        <td>
            MockServer NeoLight
        </td>
        <td>
            ✔ Same as MockServer below
        </td>
        <td>
            ✔ Split into a optimized client and server module
        </td>
        <td>
            ✔ <a href="./testcontainers/">Yes</a>
        </td>
        <td>
            ✔ <a href="./README.md#support">Yes</a>
        </td>
    </tr>
    <tr>
        <td>
            <a href="https://github.com/mock-server/mockserver">
                MockServer (original)
            </a>
        </td>
        <td>
            ✔ Provides a Java client that is used to create responses for certain requests <sup><a href="https://www.mock-server.com/">Docs</a> <a href="https://java.testcontainers.org/modules/mockserver/">Testcontainer-Docs</a></sup>
        </td>
        <td>
            ⚠ Split into a <a href="https://github.com/mock-server/mockserver/issues/1494">un-optimized</a> client and server module
        </td>
        <td>
            ✔ <a href="https://java.testcontainers.org/modules/mockserver/">Yes</a>
        </td>
        <td>
            ❌ Inactive since >1 year
        </td>
    </tr>
    <tr>
        <td>
            <a href="https://github.com/wiremock/wiremock">
                WireMock
            </a>
        </td>
        <td>
            ✔ Provides a Java API that is used to create responses for certain requests. As an alternative you can also use files to do the same <sup><a href="https://wiremock.org/docs/quickstart/java-junit/">Docs</a></sup>
        </td>
        <td>
            ❌ Monolithic - all client/server code in one module
        </td>
        <td>
            ✔ <a href="https://testcontainers.com/modules/wiremock/">Yes</a> (but only native support for file based responses)
        </td>
        <td>
            ✔ Active (dedicated support for their "Cloud" exists)
        </td>
    </tr>
    <tr>
        <td>
            <a href="https://github.com/karatelabs/karate">
                Karate
            </a>
        </td>
        <td>
            Custom programming language/syntax files <sup><a href="https://github.com/karatelabs/karate/tree/master/examples">Docs</a></sup>
        </td>
        <td>
            ❌ Monolithic - nearly all code in "core" module
        </td>
        <td>
            ❌ No
        </td>
        <td>
            ✔ Active
        </td>
    </tr>
    <tr>
        <td>
            <a href="https://github.com/mockoon/mockoon">
                Mockoon
            </a>
        </td>
        <td>
            ❌ Custom IDE <sup><a href="https://mockoon.com/docs/latest/about/">Docs</a></sup>
        </td>
        <td>
            n/a
        </td>
        <td>
            ❌ No
        </td>
        <td>
            ✔ <a href="https://mockoon.com/pro/">Yes</a>
        </td>
    </tr>
</table>

<sub>All values as of 2024-05</sub>
