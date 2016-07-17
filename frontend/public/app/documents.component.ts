import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { Document } from './document';
import { DocumentDetailComponent } from './document-detail.component';
import { DocumentService } from './document.service';

@Component({
    selector: 'my-documents',
    templateUrl: 'app/documents.component.html',
    styleUrls:  ['app/documents.component.css'],
    directives: [DocumentDetailComponent]
})

export class DocumentsComponent implements OnInit{
    title = 'Documents Overview';
    selectedDocument:Document;
    public documents: Document[];

    constructor(
        private router: Router,
        private documentService: DocumentService) {
    };

    onSelect(document: Document) { this.selectedDocument = document; };

    getDocuments() {
        this.documentService.getDocuments().then(documents => this.documents = documents);
    };

    ngOnInit() {
        this.getDocuments();
    };
    gotoDetail() {
        this.router.navigate(['/detail', this.selectedDocument.id]);
    };

}
