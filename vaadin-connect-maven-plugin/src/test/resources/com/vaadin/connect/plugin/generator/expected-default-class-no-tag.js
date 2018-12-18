import defaultClient from './connect-client.default';

export class Default {

  /**
   * Create a Default instance.
   * @param {ConnectClient=} client - an instance of ConnectClient
   */
  constructor(client = defaultClient) {
    this._client = client;
  }

  /**
   * Get all users
   *
   * @returns {Promise<array>} Return list of users
   */
  getAllUsers() {
    return this._client.call('GeneratorTestClass', 'getAllUsers', undefined, {requireCredentials: false});
  }
}

const service = new Default(defaultClient);


/**
 * Get all users
 *
 * @returns {Promise<array>} Return list of users
 */
export function getAllUsers() {
  return service.getAllUsers.apply(service, arguments);
}