import { Injectable }    from '@angular/core';
import { Http } from '@angular/http';
import 'rxjs/add/operator/toPromise';
import { Document } from './document';
@Injectable()
export class DocumentService {
    private documentsUrl = '/api/documents/';
    constructor(private http: Http) { }
    getDocuments(): Promise<Document[]> {
        return this.http.get(this.documentsUrl + "list")
            .toPromise()
            .then(response => response.json().list)
            .catch(this.handleError);
    };
    geDocument(id: number) {
        return this.http.get(this.documentsUrl + id)
            .toPromise()
            .then(response => response.json().document)
            .catch(this.handleError);
    };
    private handleError(error: any) {
        console.error('An error occurred', error);
        return Promise.reject(error.message || error);
    }
}
