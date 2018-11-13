/**
 * Vaadin Connect client class is a low-level network calling utility. Stores
 * an endpoint and facilitates remote calls to the services and methods.
 *
 * ```js
 * const client = new ConnectClient();
 * const responseData = await client.call('MyVaadinService', 'myMethod');
 * ```
 *
 * Supports an endpoint constructor option:
 * ```js
 * const client = new ConnectClient({endpoint: '/my-connect-endpont'});
 * ```
 *
 * The default endpoint is '/connect'.
 */
export class ConnectClient {
  constructor(options = {}) {
    /**
     * @property {string} endpoint="/connect" The endpoint for the remote calls.
     */
    this.endpoint = options.endpoint || '/connect';
  }

  /**
   * Makes a JSON HTTP request to the `${endpoint}/${service}/${method}`,
   * optionally supplying the provided params as JSON request body,
   * asynchronously returns the parsed JSON response data.
   *
   * @param {string} service Service class name.
   * @param {string} method Method name to call in the service class.
   * @param {Object=} params Optional object to be send in JSON request body.
   * @return {Promise<Object>} Decoded JSON response data.
   */
  async call(service, method, params) {
    if (arguments.length < 2) {
      throw new TypeError(
        `2 arguments required, but got only ${arguments.length}`
      );
    }

    /* global fetch */
    const response = await fetch(
      `${this.endpoint}/${service}/${method}`,
      {
        method: 'POST',
        headers: {
          'Accept': 'application/json',
          'Content-Type': 'application/json'
        },
        body: params !== undefined ? JSON.stringify(params) : undefined
      }
    );

    if (!response.ok) {
      throw new TypeError(
        `expected '200 OK' response, but got ${response.status}`
        + ` ${response.statusText}`
      );
    }

    return response.json();
  }
}
